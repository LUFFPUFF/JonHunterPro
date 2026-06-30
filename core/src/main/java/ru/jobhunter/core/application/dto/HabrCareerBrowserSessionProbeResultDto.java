package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.Objects;

public record HabrCareerBrowserSessionProbeResultDto(
        Status status,
        String finalUrl,
        String pageTitle,
        String diagnosticDirectory,
        Instant completedAt
) {

    public HabrCareerBrowserSessionProbeResultDto {
        Objects.requireNonNull(status, "Habr Career browser session status must not be null");
        Objects.requireNonNull(completedAt, "Habr Career browser probe completion time must not be null");

        finalUrl = requireNotBlank(finalUrl, "Habr Career browser final URL must not be blank");
        pageTitle = normalize(pageTitle);
        diagnosticDirectory = requireNotBlank(
                diagnosticDirectory,
                "Habr Career browser diagnostic directory must not be blank"
        );
    }

    public enum Status {
        AUTHENTICATED,
        AUTHENTICATION_REQUIRED,
        UNEXPECTED_PAGE
    }

    private static String requireNotBlank(String value, String message) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
