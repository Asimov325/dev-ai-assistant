package com.dev.aiassistant.config.model;

public record IntegrationConfig(String url, String username, String secret, String context) {
    public boolean complete() {
        return url != null && !url.isBlank() && username != null && !username.isBlank()
                && secret != null && !secret.isBlank();
    }
}
