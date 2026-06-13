package ru.jobhunter.core.application.dto;

import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUserDto(
        UUID id,
        String email,
        String fullName
) {

    public AuthenticatedUserDto {
        Objects.requireNonNull(id, "User id must not be null");
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(fullName, "Full name must not be null");
    }
}
