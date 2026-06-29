package ru.jobhunter.core.application.dto;

import java.time.Instant;

public record HhResumeDto(
        String id,
        String title,
        String url,
        String alternateUrl,
        String statusName,
        Instant loadedAt
) {

    public HhResumeDto {
        id = requireNotBlank(id, "HH resume id must not be blank");
        title = normalize(title);
        url = normalize(url);
        alternateUrl = normalize(alternateUrl);
        statusName = normalize(statusName);
        loadedAt = loadedAt == null ? Instant.now() : loadedAt;
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}