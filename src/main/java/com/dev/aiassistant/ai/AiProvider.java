package com.dev.aiassistant.ai;

public interface AiProvider {

    String id();

    default String modelId() {
        return "no disponible";
    }

    String generate(String prompt);
}
