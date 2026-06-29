package ru.jobhunter.infrastructure.llm.groq;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.time.Duration;

public class GroqRateLimitException extends GroqLlmException {

    public GroqRateLimitException(
            String message,
            Duration retryAfter
    ) {
        super(
                LlmFailureCategory.NETWORK_UNAVAILABLE,
                message,
                retryAfter
        );
    }
}