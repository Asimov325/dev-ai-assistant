package com.dev.aiassistant.git.model;

public record GitChangedFile(
        String changeType,
        String oldPath,
        String newPath,
        String extension,
        int linesAdded,
        int linesDeleted,
        String diff
) {
}
