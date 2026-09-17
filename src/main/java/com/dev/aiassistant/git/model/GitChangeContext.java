package com.dev.aiassistant.git.model;

import java.util.List;

public record GitChangeContext(
        String sourcePath,
        String sourceName,
        String baseBranch,
        String requirementBranch,
        String baseSha,
        String requirementSha,
        String mergeBaseSha,
        int requirementCommitCount,
        int baseCommitsNotInRequirement,
        boolean alignedWithBase,
        List<GitChangedFile> changedFiles
) {
}
