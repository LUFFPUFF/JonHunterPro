package ru.jobhunter.core.application.dto;

import java.util.Objects;
import java.util.UUID;

public record PrimaryResumeContentDto(
        UUID id,
        UUID userId,
        String title,
        String content
) {

    public PrimaryResumeContentDto {
        Objects.requireNonNull(id, "Resume id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Resume title must not be blank");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Resume content must not be blank");
        }

        title = title.trim();
        content = content.trim();
    }
}