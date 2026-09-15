package com.dev.aiassistant.git.model;

import java.util.List;

/**
 * Information discovered from a Git source selected for analysis.
 * This object does not represent a GitHub repository configuration.
 */
public record GitSourceInfo(
        String sourcePath,
        String sourceName,
        List<String> branches
) {
}
