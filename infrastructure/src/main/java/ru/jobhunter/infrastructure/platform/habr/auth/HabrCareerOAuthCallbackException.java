package ru.jobhunter.infrastructure.platform.habr.auth;

public final class HabrCareerOAuthCallbackException extends RuntimeException {

    public HabrCareerOAuthCallbackException(String message) {
        super(message);
    }

    public HabrCareerOAuthCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}