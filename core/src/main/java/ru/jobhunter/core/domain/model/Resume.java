package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public record Resume(
        ResumeId id,
        UserId userId,
        String title,
        ResumeSourceType sourceType,
        String originalFileName,
        String content,
        boolean primary,
        Instant createdAt,
        Instant updatedAt
) {

    public Resume {
        Objects.requireNonNull(id, "Resume id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(sourceType, "Resume source type must not be null");
        Objects.requireNonNull(createdAt, "Resume created timestamp must not be null");
        Objects.requireNonNull(updatedAt, "Resume updated timestamp must not be null");

        title = requireNotBlank(title, "Resume title must not be blank");
        content = requireNotBlank(content, "Resume content must not be blank");
        originalFileName = normalize(originalFileName);
    }

    public static Resume createPrimary(
            UserId userId,
            String title,
            ResumeSourceType sourceType,
            String originalFileName,
            String content
    ) {
        Instant now = Instant.now();

        return new Resume(
                ResumeId.newId(),
                userId,
                title,
                sourceType,
                originalFileName,
                content,
                true,
                now,
                now
        );
    }

    public Resume asPrimary() {
        if (primary) {
            return this;
        }

        return new Resume(
                id,
                userId,
                title,
                sourceType,
                originalFileName,
                content,
                true,
                createdAt,
                Instant.now()
        );
    }

    public Resume asNonPrimary() {
        if (!primary) {
            return this;
        }

        return new Resume(
                id,
                userId,
                title,
                sourceType,
                originalFileName,
                content,
                false,
                createdAt,
                Instant.now()
        );
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