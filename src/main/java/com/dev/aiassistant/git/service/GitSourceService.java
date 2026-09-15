package com.dev.aiassistant.git.service;

import com.dev.aiassistant.git.model.GitSourceInfo;

/**
 * Provides read-only inspection of a local Git source.
 */
public interface GitSourceService {

    GitSourceInfo inspect(String sourcePath);
}
