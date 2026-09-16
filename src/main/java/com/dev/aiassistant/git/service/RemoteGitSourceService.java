package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitSourceInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
public class RemoteGitSourceService {
    private final Path cacheRoot = Path.of(System.getProperty("java.io.tmpdir"), "development-ai-assistant", "git-cache");

    public GitSourceInfo inspect(String remoteUrl, String username, String token) {
        if (remoteUrl == null || remoteUrl.isBlank()) throw new IllegalArgumentException("La URL del repositorio remoto es obligatoria.");
        try {
            Files.createDirectories(cacheRoot);
            Path workDir = Files.createTempDirectory(cacheRoot, "check-");
            try (Git git = Git.cloneRepository().setURI(remoteUrl).setDirectory(workDir.toFile())
                    .setNoCheckout(true).setCredentialsProvider(credentials(username, token)).call()) {
                git.fetch().setRemote("origin").setCredentialsProvider(credentials(username, token)).call();
                List<String> branches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().stream()
                        .map(Ref::getName).map(name -> name.replace("refs/remotes/origin/", ""))
                        .filter(name -> !"HEAD".equals(name)).sorted().toList();
                return new GitSourceInfo(remoteUrl, repositoryName(remoteUrl), branches);
            } finally { deleteRecursively(workDir); }
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible acceder al repositorio remoto. Revisa URL, credenciales y permisos de lectura.", ex);
        }
    }

    private UsernamePasswordCredentialsProvider credentials(String username, String token) {
        return new UsernamePasswordCredentialsProvider(username == null ? "" : username, token == null ? "" : token);
    }
    private String repositoryName(String url) {
        String value = url.replaceAll("/+$", "");
        value = value.substring(value.lastIndexOf('/') + 1);
        return value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }
    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (var paths = Files.walk(path)) { paths.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} }); }
        catch (Exception ignored) {}
    }
}
