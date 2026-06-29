package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;

public record StartReadyAutoResponsesBatchCommand(
        UserId userId
) {

    public StartReadyAutoResponsesBatchCommand {
        Objects.requireNonNull(
                userId,
                "User id must not be null"
        );
    }
}