package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.GeneralCoverLetterSettings;
import ru.jobhunter.core.domain.model.UserId;

import java.time.Instant;
import java.util.Objects;

public record GeneralCoverLetterSettingsDto(
        UserId userId,
        String content,
        boolean useWhenLlmUnavailable,
        String sourceFileName,
        Instant createdAt,
        Instant updatedAt
) {

    public GeneralCoverLetterSettingsDto {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(content, "Content must not be null");
        Objects.requireNonNull(createdAt, "Created timestamp must not be null");
        Objects.requireNonNull(updatedAt, "Updated timestamp must not be null");
    }

    public static GeneralCoverLetterSettingsDto from(
            GeneralCoverLetterSettings settings
    ) {
        Objects.requireNonNull(
                settings,
                "General cover letter settings must not be null"
        );

        return new GeneralCoverLetterSettingsDto(
                settings.userId(),
                settings.content(),
                settings.useWhenLlmUnavailable(),
                settings.sourceFileName(),
                settings.createdAt(),
                settings.updatedAt()
        );
    }
}