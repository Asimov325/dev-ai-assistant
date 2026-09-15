package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitSourceInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class JGitSourceService implements GitSourceService {

    @Override
    public GitSourceInfo inspect(String sourcePath) {
        Path repositoryPath = validatePath(sourcePath);

        File gitDirectory = new File(repositoryPath.toFile(), ".git");
        if (!gitDirectory.isDirectory()) {
            throw new IllegalArgumentException("The selected path is not a Git repository: " + sourcePath);
        }

        try (var repository = new FileRepositoryBuilder()
                .setGitDir(gitDirectory)
                .readEnvironment()
                .build();
             var git = new Git(repository)) {

            List<String> branches = git.branchList()
                    .call()
                    .stream()
                    .map(Ref::getName)
                    .map(name -> name.replace("refs/heads/", ""))
                    .sorted()
                    .toList();

            return new GitSourceInfo(
                    repositoryPath.toAbsolutePath().normalize().toString(),
                    repositoryPath.getFileName().toString(),
                    branches
            );
        } catch (IOException | GitAPIException exception) {
            throw new IllegalStateException("Unable to inspect the selected Git repository: " + sourcePath, exception);
        }
    }

    private Path validatePath(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("Git repository path is required.");
        }

        Path path = Path.of(sourcePath).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Git repository path does not exist or is not a directory: " + sourcePath);
        }

        return path;
    }
}
