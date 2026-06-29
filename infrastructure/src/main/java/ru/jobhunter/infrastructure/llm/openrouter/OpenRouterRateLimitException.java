package ru.jobhunter.infrastructure.llm.openrouter;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.time.Duration;
import java.util.Objects;

public final class OpenRouterRateLimitException
        extends OpenRouterLlmException {

    private final String model;

    public OpenRouterRateLimitException(
            String model,
            String message,
            Duration retryAfter
    ) {
        super(
                OpenRouterCircuitKey.forModel(model),
                LlmFailureCategory.OPENROUTER_RATE_LIMIT,
                Objects.requireNonNull(
                        message,
                        "OpenRouter rate limit message must not be null"
                ),
                null,
                retryAfter
        );

        this.model = normalizeModel(model);
    }

    public String model() {
        return model;
    }

    private static String normalizeModel(String value) {
        Objects.requireNonNull(
                value,
                "OpenRouter model must not be null"
        );

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "OpenRouter model must not be blank"
            );
        }

        return normalized;
    }
}