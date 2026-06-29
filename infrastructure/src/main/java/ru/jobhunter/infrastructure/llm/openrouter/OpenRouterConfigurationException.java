package ru.jobhunter.infrastructure.llm.openrouter;

public class OpenRouterConfigurationException extends RuntimeException {

    public OpenRouterConfigurationException(String message) {
        super(message);
    }

    public OpenRouterConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
