package ru.jobhunter.infrastructure.llm.ollama;

public class OllamaConfigurationException extends RuntimeException {

    public OllamaConfigurationException(String message) {
        super(message);
    }

    public OllamaConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
