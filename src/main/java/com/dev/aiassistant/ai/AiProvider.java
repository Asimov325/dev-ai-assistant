package com.dev.aiassistant.ai;

public interface AiProvider {

    String id();

    String generate(String prompt);
}
