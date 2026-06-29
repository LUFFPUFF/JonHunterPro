package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class GeneralCoverLetterSettings {

    public static final int MAX_CONTENT_LENGTH = 4_000;
    public static final int MAX_SOURCE_FILE_NAME_LENGTH = 255;

    private final UserId userId;
    private final String content;
    private final boolean useWhenLlmUnavailable;
    private final String sourceFileName;
    private final Instant createdAt;
    private final Instant updatedAt;

    private GeneralCoverLetterSettings(
            UserId userId,
            String content,
            boolean useWhenLlmUnavailable,
            String sourceFileName,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.userId = Objects.requireNonNull(
                userId,
                "User id must not be null"
        );
        this.content = normalizeContent(content);
        this.useWhenLlmUnavailable = useWhenLlmUnavailable;
        this.sourceFileName = normalizeSourceFileName(sourceFileName);
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Created timestamp must not be null"
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Updated timestamp must not be null"
        );

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Updated timestamp must not be before created timestamp"
            );
        }
    }

    public static GeneralCoverLetterSettings create(
            UserId userId,
            String content,
            boolean useWhenLlmUnavailable,
            String sourceFileName
    ) {
        Instant now = Instant.now();

        return new GeneralCoverLetterSettings(
                userId,
                content,
                useWhenLlmUnavailable,
                sourceFileName,
                now,
                now
        );
    }

    public static GeneralCoverLetterSettings restore(
            UserId userId,
            String content,
            boolean useWhenLlmUnavailable,
            String sourceFileName,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new GeneralCoverLetterSettings(
                userId,
                content,
                useWhenLlmUnavailable,
                sourceFileName,
                createdAt,
                updatedAt
        );
    }

    public GeneralCoverLetterSettings update(
            String newContent,
            boolean newUseWhenLlmUnavailable,
            String newSourceFileName
    ) {
        return new GeneralCoverLetterSettings(
                userId,
                newContent,
                newUseWhenLlmUnavailable,
                newSourceFileName,
                createdAt,
                Instant.now()
        );
    }

    public UserId userId() {
        return userId;
    }

    public String content() {
        return content;
    }

    public boolean useWhenLlmUnavailable() {
        return useWhenLlmUnavailable;
    }

    public String sourceFileName() {
        return sourceFileName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static String normalizeContent(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "General cover letter content must not be blank"
            );
        }

        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();

        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "General cover letter content must not exceed "
                            + MAX_CONTENT_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeSourceFileName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_SOURCE_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "General cover letter source file name must not exceed "
                            + MAX_SOURCE_FILE_NAME_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }
}