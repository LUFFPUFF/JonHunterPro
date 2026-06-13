package ru.jobhunter.infrastructure.platform.hh.auth;

public final class HhOAuthCallbackException extends RuntimeException {

    public HhOAuthCallbackException(String message) {
        super(message);
    }

    public HhOAuthCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
