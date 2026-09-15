package com.dev.aiassistant.git.model;

import java.util.List;

public record GitChangeContext(
        String sourcePath,
        String sourceName,
        String baseBranch,
        String requirementBranch,
        List<GitChangedFile> changedFiles
) {
}
