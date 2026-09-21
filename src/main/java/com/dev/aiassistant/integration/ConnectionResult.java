package com.dev.aiassistant.integration;

public record ConnectionResult(boolean success, String message) {
    public static ConnectionResult ok(String message) {
        return new ConnectionResult(true, message);
    }

    public static ConnectionResult error(String message) {
        return new ConnectionResult(false, message);
    }
}
