package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitSourceInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
public class RemoteGitSourceService {

    private final JGitSourceService localGit;

    public RemoteGitSourceService(JGitSourceService localGit) {
        this.localGit = localGit;
    }

    public GitSourceInfo inspect(String remoteUrl, String username, String token) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new IllegalArgumentException("La URL del repositorio remoto es obligatoria.");
        }
        try {
            var refs = Git.lsRemoteRepository()
                    .setRemote(remoteUrl.trim())
                    .setHeads(true)
                    .setTags(false)
                    .setCredentialsProvider(credentials(username, token))
                    .call();
            List<String> branches = refs.stream()
                    .map(Ref::getName)
                    .filter(name -> name.startsWith("refs/heads/"))
                    .map(name -> name.substring("refs/heads/".length()))
                    .sorted()
                    .toList();
            if (branches.isEmpty()) {
                throw new IllegalStateException("El repositorio respondió, pero no se encontraron ramas accesibles.");
            }
            return new GitSourceInfo(remoteUrl.trim(), repositoryName(remoteUrl), branches);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible acceder al repositorio remoto. Revisa URL, credenciales y permisos de lectura.", ex);
        }
    }

    public GitChangeContext compare(String remoteUrl, String username, String token, String baseBranch, String requirementBranch) {
        Path temp = null;
        try {
            temp = Files.createTempDirectory("dev-ai-analysis-");
            var auth = credentials(username, token);
            try (Git git = Git.init().setDirectory(temp.toFile()).call()) {
                git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(remoteUrl.trim())).call();
                fetchBranch(git, auth, baseBranch);
                if (!baseBranch.equals(requirementBranch)) fetchBranch(git, auth, requirementBranch);
            }
            return localGit.compare(temp.toString(), baseBranch, requirementBranch);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible comparar las ramas remotas. Revisa ramas, credenciales y conectividad.", ex);
        } finally {
            deleteQuietly(temp);
        }
    }

    private void fetchBranch(Git git, UsernamePasswordCredentialsProvider auth, String branch) throws Exception {
        String source = "refs/heads/" + branch;
        String target = "refs/remotes/origin/" + branch;
        git.fetch()
                .setRemote("origin")
                .setCredentialsProvider(auth)
                .setRefSpecs(new RefSpec("+" + source + ":" + target))
                .call();
    }

    private void deleteQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private UsernamePasswordCredentialsProvider credentials(String username, String token) {
        return new UsernamePasswordCredentialsProvider(username == null ? "" : username.trim(), token == null ? "" : token);
    }

    private String repositoryName(String url) {
        String value = url.trim().replaceAll("/+$", "");
        value = value.substring(value.lastIndexOf('/') + 1);
        return value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }
}
