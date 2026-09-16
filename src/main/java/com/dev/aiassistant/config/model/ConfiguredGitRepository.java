package com.dev.aiassistant.config.model;

import java.util.List;

public record ConfiguredGitRepository(String name, String source, String location, String username, String secret, List<String> branches) {
    public boolean remote() { return "REMOTE".equalsIgnoreCase(source); }
    public String key() { return source + ":" + location; }
}
