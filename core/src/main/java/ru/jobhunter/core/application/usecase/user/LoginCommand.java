package ru.jobhunter.core.application.usecase.user;

import java.util.Objects;

public record LoginCommand(
        String email,
        String rawPassword
) {

    public LoginCommand {
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(rawPassword, "Password must not be null");
    }
}
