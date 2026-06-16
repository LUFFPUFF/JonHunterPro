package ru.jobhunter.infrastructure.platform.habr.auth;

public class HabrCareerOAuthTokenRequestException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public HabrCareerOAuthTokenRequestException(
            String message,
            int statusCode,
            String responseBody
    ) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HabrCareerOAuthTokenRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}