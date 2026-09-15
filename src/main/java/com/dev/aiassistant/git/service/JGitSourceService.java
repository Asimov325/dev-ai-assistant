package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.git.model.GitSourceInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class JGitSourceService implements GitSourceService {

    @Override
    public GitSourceInfo inspect(String sourcePath) {
        Path repositoryPath = validatePath(sourcePath);

        try (Repository repository = openRepository(repositoryPath);
             Git git = new Git(repository)) {

            List<String> branches = git.branchList()
                    .call()
                    .stream()
                    .map(Ref::getName)
                    .map(name -> name.replace("refs/heads/", ""))
                    .sorted()
                    .toList();

            return new GitSourceInfo(
                    repositoryPath.toString(),
                    repositoryPath.getFileName().toString(),
                    branches
            );
        } catch (IOException | GitAPIException exception) {
            throw new IllegalStateException("Unable to inspect the selected Git repository: " + sourcePath, exception);
        }
    }

    @Override
    public GitChangeContext compare(String sourcePath, String baseBranch, String requirementBranch) {
        Path repositoryPath = validatePath(sourcePath);
        validateBranchName(baseBranch, "Base branch");
        validateBranchName(requirementBranch, "Requirement branch");

        try (Repository repository = openRepository(repositoryPath);
             Git git = new Git(repository)) {

            ObjectId base = resolveBranch(repository, baseBranch);
            ObjectId requirement = resolveBranch(repository, requirementBranch);

            List<DiffEntry> entries = git.diff()
                    .setOldTree(prepareTreeParser(repository, base))
                    .setNewTree(prepareTreeParser(repository, requirement))
                    .setShowNameAndStatusOnly(false)
                    .call();

            List<GitChangedFile> changedFiles = new ArrayList<>();
            for (DiffEntry entry : entries) {
                changedFiles.add(toChangedFile(repository, entry));
            }

            return new GitChangeContext(
                    repositoryPath.toString(),
                    repositoryPath.getFileName().toString(),
                    baseBranch,
                    requirementBranch,
                    List.copyOf(changedFiles)
            );
        } catch (IOException | GitAPIException exception) {
            throw new IllegalStateException(
                    "Unable to compare branches '" + baseBranch + "' and '" + requirementBranch + "'.",
                    exception
            );
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository);
             ObjectReader reader = repository.newObjectReader()) {
            var commit = walk.parseCommit(objectId);
            var tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser parser = new CanonicalTreeParser();
            parser.reset(reader, tree.getId());
            return parser;
        }
    }

    private GitChangedFile toChangedFile(Repository repository, DiffEntry entry) throws IOException {
        int linesAdded = 0;
        int linesDeleted = 0;

        try (DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            formatter.setRepository(repository);
            formatter.setDetectRenames(true);
            FileHeader header = formatter.toFileHeader(entry);
            for (Edit edit : header.toEditList()) {
                linesDeleted += edit.getEndA() - edit.getBeginA();
                linesAdded += edit.getEndB() - edit.getBeginB();
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(output)) {
            formatter.setRepository(repository);
            formatter.setDetectRenames(true);
            formatter.format(entry);
        }

        String relevantPath = entry.getNewPath().equals(DiffEntry.DEV_NULL) ? entry.getOldPath() : entry.getNewPath();

        return new GitChangedFile(
                entry.getChangeType().name(),
                normalizeDiffPath(entry.getOldPath()),
                normalizeDiffPath(entry.getNewPath()),
                extractExtension(relevantPath),
                linesAdded,
                linesDeleted,
                output.toString(StandardCharsets.UTF_8)
        );
    }

    private ObjectId resolveBranch(Repository repository, String branchName) throws IOException {
        ObjectId objectId = repository.resolve(branchName + "^{commit}");
        if (objectId == null) {
            objectId = repository.resolve("refs/remotes/origin/" + branchName + "^{commit}");
        }
        if (objectId == null) {
            throw new IllegalArgumentException("Git branch was not found: " + branchName);
        }
        return objectId;
    }

    private Repository openRepository(Path repositoryPath) throws IOException {
        File gitDirectory = new File(repositoryPath.toFile(), ".git");
        if (!gitDirectory.isDirectory()) {
            throw new IllegalArgumentException("The selected path is not a Git repository: " + repositoryPath);
        }

        return new FileRepositoryBuilder()
                .setGitDir(gitDirectory)
                .readEnvironment()
                .build();
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

    private void validateBranchName(String branchName, String label) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    private String normalizeDiffPath(String path) {
        return DiffEntry.DEV_NULL.equals(path) ? null : path;
    }

    private String extractExtension(String path) {
        if (path == null || DiffEntry.DEV_NULL.equals(path)) {
            return "";
        }
        String fileName = Path.of(path).getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot).toLowerCase() : "";
    }
}
