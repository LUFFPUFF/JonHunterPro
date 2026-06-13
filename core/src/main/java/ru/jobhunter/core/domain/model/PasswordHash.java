package ru.jobhunter.core.domain.model;

import java.util.Objects;

public record PasswordHash(String value) {

    private static final int MIN_LENGTH = 20;
    private static final int MAX_LENGTH = 255;

    public PasswordHash {
        Objects.requireNonNull(value, "Password hash must not be null");

        String normalized = value.trim();

        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password hash is too short");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password hash must not exceed " + MAX_LENGTH + " characters");
        }

        value = normalized;
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
