package ru.jobhunter.infrastructure.llm.routing;

import ru.jobhunter.infrastructure.llm.openrouter.OpenRouterCircuitKey;

import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class LlmCircuitBreakerFailurePolicy {

    private static final String OLLAMA_PROVIDER = "ollama";

    private final Duration ollamaRuntimeCrashCooldown;
    private final Duration openRouterRateLimitFallbackCooldown;

    LlmCircuitBreakerFailurePolicy(
            LlmCircuitBreakerProperties properties
    ) {
        Objects.requireNonNull(
                properties,
                "LLM circuit breaker properties must not be null"
        );

        this.ollamaRuntimeCrashCooldown =
                properties.resolvedOllamaRuntimeCrashCooldown();

        this.openRouterRateLimitFallbackCooldown =
                properties.resolvedOpenRouterRateLimitFallbackCooldown();
    }

    Optional<CircuitOpenDecision> resolve(
            LlmProviderUnavailableException failure
    ) {
        Objects.requireNonNull(
                failure,
                "LLM provider failure must not be null"
        );

        Set<Throwable> visited = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );

        Throwable current = failure;

        while (current != null && visited.add(current)) {
            if (current instanceof LlmProviderUnavailableException unavailable) {
                Optional<CircuitOpenDecision> decision =
                        resolveUnavailableFailure(unavailable);

                if (decision.isPresent()) {
                    return decision;
                }
            }

            current = current.getCause();
        }

        return Optional.empty();
    }

    private Optional<CircuitOpenDecision> resolveUnavailableFailure(
            LlmProviderUnavailableException failure
    ) {
        if (OLLAMA_PROVIDER.equalsIgnoreCase(failure.providerId())
                && failure.failureCategory()
                == LlmFailureCategory.OLLAMA_RUNTIME_CRASH) {

            return Optional.of(
                    new CircuitOpenDecision(
                            OLLAMA_PROVIDER,
                            LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                            ollamaRuntimeCrashCooldown
                    )
            );
        }

        if (OpenRouterCircuitKey.isModelCircuitKey(
                failure.providerId()
        ) && failure.failureCategory()
                == LlmFailureCategory.OPENROUTER_RATE_LIMIT) {

            Duration retryAfter = failure.retryAfter();

            Duration cooldown = retryAfter.isZero()
                    ? openRouterRateLimitFallbackCooldown
                    : retryAfter;

            return Optional.of(
                    new CircuitOpenDecision(
                            failure.providerId(),
                            LlmFailureCategory.OPENROUTER_RATE_LIMIT,
                            cooldown
                    )
            );
        }

        return Optional.empty();
    }

    record CircuitOpenDecision(
            String providerId,
            LlmFailureCategory failureCategory,
            Duration cooldown
    ) {

        CircuitOpenDecision {
            Objects.requireNonNull(
                    providerId,
                    "Provider id must not be null"
            );
            Objects.requireNonNull(
                    failureCategory,
                    "Failure category must not be null"
            );
            Objects.requireNonNull(
                    cooldown,
                    "Cooldown must not be null"
            );

            if (cooldown.isZero() || cooldown.isNegative()) {
                throw new IllegalArgumentException(
                        "Circuit breaker cooldown must be positive"
                );
            }
        }
    }
}