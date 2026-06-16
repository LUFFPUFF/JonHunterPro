package ru.jobhunter.infrastructure.platform.habr.auth;

public class HabrCareerOAuthConfigurationException extends RuntimeException {

    public HabrCareerOAuthConfigurationException(String message) {
        super(message);
    }

    public HabrCareerOAuthConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
