package ru.jobhunter.infrastructure.platform.habr.api;

public class HabrCareerApiConfigurationException extends RuntimeException {

    public HabrCareerApiConfigurationException(String message) {
        super(message);
    }

    public HabrCareerApiConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
