package ru.jobhunter.infrastructure.llm.openrouter;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

import java.time.Duration;

public class OpenRouterLlmException
        extends LlmProviderUnavailableException {

    private static final String PROVIDER = "openrouter";

    public OpenRouterLlmException(
            LlmFailureCategory failureCategory,
            String message
    ) {
        super(PROVIDER, failureCategory, message);
    }

    public OpenRouterLlmException(
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause
    ) {
        super(PROVIDER, failureCategory, message, cause);
    }

    protected OpenRouterLlmException(
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause,
            Duration retryAfter
    ) {
        this(
                PROVIDER,
                failureCategory,
                message,
                cause,
                retryAfter
        );
    }

    protected OpenRouterLlmException(
            String providerId,
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause,
            Duration retryAfter
    ) {
        super(
                providerId,
                failureCategory,
                message,
                cause,
                retryAfter
        );
    }
}