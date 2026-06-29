package ru.jobhunter.infrastructure.llm.routing;

import java.time.Instant;
import java.util.Objects;

public record LlmProviderCircuitOpenState(
        String providerId,
        LlmFailureCategory failureCategory,
        Instant openUntil
) {

    public LlmProviderCircuitOpenState {
        providerId = normalizeProviderId(providerId);
        failureCategory = Objects.requireNonNull(
                failureCategory,
                "LLM failure category must not be null"
        );
        openUntil = Objects.requireNonNull(
                openUntil,
                "Circuit open time must not be null"
        );
    }

    private static String normalizeProviderId(String value) {
        Objects.requireNonNull(
                value,
                "LLM provider id must not be null"
        );

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM provider id must not be blank"
            );
        }

        return normalized;
    }
}