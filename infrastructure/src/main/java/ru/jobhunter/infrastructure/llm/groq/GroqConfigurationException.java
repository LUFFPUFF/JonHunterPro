package ru.jobhunter.infrastructure.llm.groq;

public class GroqConfigurationException extends RuntimeException {

    public GroqConfigurationException(String message) {
        super(message);
    }
}