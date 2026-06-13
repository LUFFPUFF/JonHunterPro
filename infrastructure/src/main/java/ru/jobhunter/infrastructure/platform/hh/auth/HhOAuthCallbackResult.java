package ru.jobhunter.infrastructure.platform.hh.auth;

import java.util.Objects;

public record HhOAuthCallbackResult(
        String code,
        String state
) {

    public HhOAuthCallbackResult {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code must not be blank");
        }

        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("OAuth state must not be blank");
        }

        code = code.trim();
        state = state.trim();

        Objects.requireNonNull(code, "Authorization code must not be null");
        Objects.requireNonNull(state, "OAuth state must not be null");
    }
}
