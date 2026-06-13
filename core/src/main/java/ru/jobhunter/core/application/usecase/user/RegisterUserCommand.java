package ru.jobhunter.core.application.usecase.user;

import java.util.Objects;

public record RegisterUserCommand(
        String email,
        String rawPassword,
        String fullName
) {

    public RegisterUserCommand {
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(rawPassword, "Password must not be null");
        Objects.requireNonNull(fullName, "Full name must not be null");
    }
}
