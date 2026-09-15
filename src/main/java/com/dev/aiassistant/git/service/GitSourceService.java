package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitSourceInfo;

/**
 * Provides read-only inspection and comparison of a local Git source.
 */
public interface GitSourceService {

    GitSourceInfo inspect(String sourcePath);

    GitChangeContext compare(String sourcePath, String baseBranch, String requirementBranch);
}
