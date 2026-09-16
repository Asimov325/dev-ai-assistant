package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitSourceInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RemoteGitSourceService {

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

    private UsernamePasswordCredentialsProvider credentials(String username, String token) {
        return new UsernamePasswordCredentialsProvider(username == null ? "" : username.trim(), token == null ? "" : token);
    }

    private String repositoryName(String url) {
        String value = url.trim().replaceAll("/+$", "");
        value = value.substring(value.lastIndexOf('/') + 1);
        return value.endsWith(".git") ? value.substring(0, value.length() - 4) : value;
    }
}
