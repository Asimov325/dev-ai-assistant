package com.dev.aiassistant.config.model;

import java.util.List;

public record ConfiguredGitRepository(String name, String path, List<String> branches) {
}
