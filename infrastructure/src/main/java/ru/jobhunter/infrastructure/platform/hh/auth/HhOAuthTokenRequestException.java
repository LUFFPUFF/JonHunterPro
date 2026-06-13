package ru.jobhunter.infrastructure.platform.hh.auth;

public final class HhOAuthTokenRequestException extends RuntimeException {

    public HhOAuthTokenRequestException(String message) {
        super(message);
    }

    public HhOAuthTokenRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
