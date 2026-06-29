package ru.jobhunter.infrastructure.llm.routing;

import java.time.Duration;
import java.util.Objects;

public class LlmProviderUnavailableException extends RuntimeException {

    private final String providerId;
    private final LlmFailureCategory failureCategory;
    private final Duration retryAfter;

    public LlmProviderUnavailableException(
            String providerId,
            LlmFailureCategory failureCategory,
            String message
    ) {
        this(
                providerId,
                failureCategory,
                message,
                null,
                Duration.ZERO
        );
    }

    public LlmProviderUnavailableException(
            String providerId,
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause
    ) {
        this(
                providerId,
                failureCategory,
                message,
                cause,
                Duration.ZERO
        );
    }

    public LlmProviderUnavailableException(
            String providerId,
            LlmFailureCategory failureCategory,
            String message,
            Duration retryAfter
    ) {
        this(
                providerId,
                failureCategory,
                message,
                null,
                retryAfter
        );
    }

    public LlmProviderUnavailableException(
            String providerId,
            LlmFailureCategory failureCategory,
            String message,
            Throwable cause,
            Duration retryAfter
    ) {
        super(requireMessage(message), cause);

        this.providerId = requireProviderId(providerId);
        this.failureCategory = requireFailureCategory(failureCategory);
        this.retryAfter = normalizeRetryAfter(retryAfter);
    }

    public String providerId() {
        return providerId;
    }

    public LlmFailureCategory failureCategory() {
        return failureCategory;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    private static String requireProviderId(String value) {
        Objects.requireNonNull(value, "LLM provider id must not be null");

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM provider id must not be blank"
            );
        }

        return normalized;
    }

    private static LlmFailureCategory requireFailureCategory(
            LlmFailureCategory value
    ) {
        return Objects.requireNonNull(
                value,
                "LLM failure category must not be null"
        );
    }

    private static String requireMessage(String value) {
        Objects.requireNonNull(value, "LLM failure message must not be null");

        String normalized = value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM failure message must not be blank"
            );
        }

        return normalized;
    }

    private static Duration normalizeRetryAfter(Duration value) {
        if (value == null || value.isNegative()) {
            return Duration.ZERO;
        }

        return value;
    }
}