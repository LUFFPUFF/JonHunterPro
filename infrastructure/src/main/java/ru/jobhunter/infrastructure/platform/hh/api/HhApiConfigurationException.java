package ru.jobhunter.infrastructure.platform.hh.api;

public class HhApiConfigurationException extends RuntimeException {

    public HhApiConfigurationException(String message) {
        super(message);
    }

    public HhApiConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
