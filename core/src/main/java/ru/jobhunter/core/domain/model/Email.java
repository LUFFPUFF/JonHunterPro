package ru.jobhunter.core.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final int MAX_EMAIL_LENGTH = 320;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    public Email {
        Objects.requireNonNull(value, "Email must not be null");

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email length must not exceed " + MAX_EMAIL_LENGTH + " characters");
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email has invalid format");
        }

        value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
