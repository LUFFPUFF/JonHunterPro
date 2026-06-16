package ru.jobhunter.infrastructure.platform.habr.auth;

public record HabrCareerOAuthCallbackResult(
        String code,
        String state
) {

    public HabrCareerOAuthCallbackResult {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Habr Career authorization code must not be blank");
        }

        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("Habr Career OAuth state must not be blank");
        }

        code = code.trim();
        state = state.trim();
    }
}