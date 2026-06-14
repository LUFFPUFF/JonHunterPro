package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AuthProvider;

import java.time.Instant;
import java.util.Objects;

public record HhCurrentUserDto(
        AuthProvider provider,
        String externalUserId,
        String email,
        String firstName,
        String lastName,
        String middleName,
        String userType,
        Boolean admin,
        Instant loadedAt
) {

    public HhCurrentUserDto {
        Objects.requireNonNull(provider, "Auth provider must not be null");
        Objects.requireNonNull(loadedAt, "Loaded timestamp must not be null");
    }
}
