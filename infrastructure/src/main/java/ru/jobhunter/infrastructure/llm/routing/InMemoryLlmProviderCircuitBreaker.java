package ru.jobhunter.infrastructure.llm.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class InMemoryLlmProviderCircuitBreaker
        implements LlmProviderCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(
            InMemoryLlmProviderCircuitBreaker.class
    );

    private final LlmCircuitBreakerFailurePolicy failurePolicy;
    private final Clock clock;
    private final ConcurrentMap<String, LlmProviderCircuitOpenState>
            openCircuits = new ConcurrentHashMap<>();

    InMemoryLlmProviderCircuitBreaker(
            LlmCircuitBreakerFailurePolicy failurePolicy,
            Clock clock
    ) {
        this.failurePolicy = Objects.requireNonNull(
                failurePolicy,
                "LLM circuit breaker failure policy must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "Clock must not be null"
        );
    }

    @Override
    public Optional<LlmProviderCircuitOpenState> openState(
            String providerId
    ) {
        String normalizedProviderId = normalizeProviderId(providerId);

        LlmProviderCircuitOpenState state = openCircuits.get(
                normalizedProviderId
        );

        if (state == null) {
            return Optional.empty();
        }

        Instant now = clock.instant();

        if (state.openUntil().isAfter(now)) {
            return Optional.of(state);
        }

        boolean removed = openCircuits.remove(
                normalizedProviderId,
                state
        );

        if (removed) {
            log.info(
                    "LLM provider circuit cooldown expired: provider={}, "
                            + "failureCategory={}",
                    normalizedProviderId,
                    state.failureCategory()
            );
        }

        return Optional.empty();
    }

    @Override
    public void recordSuccess(String providerId) {
        String normalizedProviderId = normalizeProviderId(providerId);

        LlmProviderCircuitOpenState removed = openCircuits.remove(
                normalizedProviderId
        );

        if (removed != null) {
            log.info(
                    "LLM provider circuit closed after successful request: "
                            + "provider={}",
                    normalizedProviderId
            );
        }
    }

    @Override
    public void recordFailure(
            LlmProviderUnavailableException failure
    ) {
        failurePolicy.resolve(failure).ifPresent(this::openCircuit);
    }

    private void openCircuit(
            LlmCircuitBreakerFailurePolicy.CircuitOpenDecision decision
    ) {
        Instant openUntil = clock.instant().plus(decision.cooldown());

        LlmProviderCircuitOpenState candidate =
                new LlmProviderCircuitOpenState(
                        decision.providerId(),
                        decision.failureCategory(),
                        openUntil
                );

        LlmProviderCircuitOpenState actual = openCircuits.compute(
                candidate.providerId(),
                (ignored, existing) -> existing == null
                        || existing.openUntil().isBefore(
                        candidate.openUntil()
                )
                        ? candidate
                        : existing
        );

        log.warn(
                "LLM provider circuit opened: provider={}, "
                        + "failureCategory={}, cooldownSeconds={}, openUntil={}",
                actual.providerId(),
                actual.failureCategory(),
                decision.cooldown().toSeconds(),
                actual.openUntil()
        );
    }

    private String normalizeProviderId(String providerId) {
        Objects.requireNonNull(
                providerId,
                "LLM provider id must not be null"
        );

        String normalized = providerId.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM provider id must not be blank"
            );
        }

        return normalized;
    }
}