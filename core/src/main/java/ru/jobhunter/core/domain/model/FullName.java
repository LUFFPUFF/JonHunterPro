package ru.jobhunter.core.domain.model;

import java.util.Objects;

public record FullName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 255;

    public FullName {
        Objects.requireNonNull(value, "Full name must not be null");

        String normalized = value.trim().replaceAll("\\s+", " ");

        if (normalized.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Full name must contain at least " + MIN_LENGTH + " characters");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Full name must not exceed " + MAX_LENGTH + " characters");
        }

        value = normalized;
    }

    public static FullName of(String value) {
        return new FullName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
