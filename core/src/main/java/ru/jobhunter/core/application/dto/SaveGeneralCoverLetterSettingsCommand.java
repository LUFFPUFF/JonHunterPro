package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;

public record SaveGeneralCoverLetterSettingsCommand(
        UserId userId,
        String content,
        boolean useWhenLlmUnavailable,
        String sourceFileName
) {

    public SaveGeneralCoverLetterSettingsCommand {
        Objects.requireNonNull(userId, "User id must not be null");
    }
}