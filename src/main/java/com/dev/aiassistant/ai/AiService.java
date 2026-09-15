package com.dev.aiassistant.ai;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final AiProvider aiProvider;

    public AiService(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String providerId() {
        return aiProvider.id();
    }

    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("El texto para la IA es obligatorio.");
        }
        return aiProvider.generate(prompt.trim());
    }
}
