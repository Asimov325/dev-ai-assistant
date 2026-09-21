package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitSourceInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
public class RemoteGitSourceService {
    private static final Logger log = LoggerFactory.getLogger(RemoteGitSourceService.class);

    private final JGitSourceService localGit;

    public RemoteGitSourceService(JGitSourceService localGit) {
        this.localGit = localGit;
    }

    public GitSourceInfo inspect(String remoteUrl, String username, String token) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new IllegalArgumentException("La URL del repositorio remoto es obligatoria.");
        }
        long start = System.currentTimeMillis();
        log.info("Git REMOTE: inicio inspección. repositorio={} usuarioInformado={}", safeRepository(remoteUrl), username != null && !username.isBlank());
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
            log.info("Git REMOTE: inspección completada. repositorio={} ramas={} tiempoMs={}", repositoryName(remoteUrl), branches.size(), System.currentTimeMillis() - start);
            return new GitSourceInfo(remoteUrl.trim(), repositoryName(remoteUrl), branches);
        } catch (Exception ex) {
            log.error("Git REMOTE: error inspeccionando repositorio. repositorio={} tipo={} mensaje={} tiempoMs={}",
                    safeRepository(remoteUrl), ex.getClass().getName(), safeMessage(ex), System.currentTimeMillis() - start, ex);
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible acceder al repositorio remoto. Revisa URL, credenciales y permisos de lectura.", ex);
        }
    }

    public GitChangeContext compare(String remoteUrl, String username, String token, String baseBranch, String requirementBranch) {
        Path temp = null;
        long start = System.currentTimeMillis();
        log.info("Git REMOTE: inicio comparación. repositorio={} ramaOrigen={} ramaRequerimiento={}", safeRepository(remoteUrl), baseBranch, requirementBranch);
        try {
            temp = Files.createTempDirectory("dev-ai-analysis-");
            var auth = credentials(username, token);
            try (Git git = Git.init().setDirectory(temp.toFile()).call()) {
                git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(remoteUrl.trim())).call();
                fetchBranch(git, auth, baseBranch);
                if (!baseBranch.equals(requirementBranch)) fetchBranch(git, auth, requirementBranch);
            }
            GitChangeContext result = localGit.compare(temp.toString(), baseBranch, requirementBranch);
            log.info("Git REMOTE: comparación completada. repositorio={} ramaOrigen={} ramaRequerimiento={} archivos={} tiempoMs={}",
                    safeRepository(remoteUrl), baseBranch, requirementBranch, result.changedFiles().size(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("Git REMOTE: error comparando ramas. repositorio={} ramaOrigen={} ramaRequerimiento={} tipo={} mensaje={} tiempoMs={}",
                    safeRepository(remoteUrl), baseBranch, requirementBranch, ex.getClass().getName(), safeMessage(ex), System.currentTimeMillis() - start, ex);
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
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private UsernamePasswordCredentialsProvider credentials(String username, String token) {
        return new UsernamePasswordCredentialsProvider(username == null ? "" : username.trim(), token == null ? "" : token);
    }

    private String safeRepository(String url) {
        if (url == null || url.isBlank()) return "(vacío)";
        try {
            return repositoryName(url);
        } catch (RuntimeException ignored) {
            return "(URL no válida)";
        }
    }

    private String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "(sin mensaje)";
        return message.replaceAll("(?i)(https?://)[^/@\\s]+@", "$1***@");
    }

    private String repositoryName(String url) {
        String value = url.trim().replaceAll("/+$", "");
        value = value.substring(value.lastIndexOf('/') + 1);
        return value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }
}
