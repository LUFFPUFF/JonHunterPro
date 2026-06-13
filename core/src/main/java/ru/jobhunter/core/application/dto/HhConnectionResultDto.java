package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AuthProvider;

import java.time.Instant;
import java.util.Objects;

public record HhConnectionResultDto(
        AuthProvider provider,
        Instant connectedAt
) {

    public HhConnectionResultDto {
        Objects.requireNonNull(provider, "Auth provider must not be null");
        Objects.requireNonNull(connectedAt, "Connected timestamp must not be null");
    }
}
