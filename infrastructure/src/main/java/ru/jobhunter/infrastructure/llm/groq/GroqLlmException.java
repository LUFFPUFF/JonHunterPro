package ru.jobhunter.infrastructure.llm.groq;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

import java.time.Duration;

public class GroqLlmException
        extends LlmProviderUnavailableException {

    public static final String PROVIDER_ID = "groq";

    public GroqLlmException(
            LlmFailureCategory failureCategory,
            String message
    ) {
        super(PROVIDER_ID, failureCategory, message);
    }

    public GroqLlmException(
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause
    ) {
        super(PROVIDER_ID, failureCategory, message, cause);
    }

    protected GroqLlmException(
            LlmFailureCategory failureCategory,
            String message,
            Duration retryAfter
    ) {
        super(PROVIDER_ID, failureCategory, message, retryAfter);
    }
}