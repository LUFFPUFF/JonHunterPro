package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.ResumeSourceType;

import java.time.Instant;
import java.util.UUID;

public record ResumeDto(
        UUID id,
        UUID userId,
        String title,
        ResumeSourceType sourceType,
        String originalFileName,
        boolean primary,
        Instant createdAt,
        Instant updatedAt
) {
}