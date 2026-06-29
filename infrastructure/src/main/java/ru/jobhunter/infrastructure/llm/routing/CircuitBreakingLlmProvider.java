package ru.jobhunter.infrastructure.llm.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class CircuitBreakingLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(
            CircuitBreakingLlmProvider.class
    );

    private final LlmProvider delegate;
    private final LlmProviderCircuitBreaker circuitBreaker;
    private final Clock clock;

    public CircuitBreakingLlmProvider(
            LlmProvider delegate,
            LlmProviderCircuitBreaker circuitBreaker,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(
                delegate,
                "Delegate LLM provider must not be null"
        );
        this.circuitBreaker = Objects.requireNonNull(
                circuitBreaker,
                "LLM provider circuit breaker must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "Clock must not be null"
        );
    }

    @Override
    public String providerId() {
        return delegate.providerId();
    }

    @Override
    public CompletableFuture<LlmGenerationResponse> generate(
            LlmGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "LLM generation request must not be null"
        );

        Optional<LlmProviderCircuitOpenState> openState =
                circuitBreaker.openState(providerId());

        if (openState.isPresent()) {
            return CompletableFuture.failedFuture(
                    circuitOpenFailure(openState.get())
            );
        }

        try {
            CompletableFuture<LlmGenerationResponse> responseFuture =
                    Objects.requireNonNull(
                            delegate.generate(request),
                            "LLM provider must not return a null future"
                    );

            return responseFuture.whenComplete(
                    (response, throwable) -> {
                        if (throwable == null) {
                            circuitBreaker.recordSuccess(providerId());
                            return;
                        }

                        recordProviderFailure(throwable);
                    }
            );
        } catch (Throwable throwable) {
            recordProviderFailure(throwable);

            return CompletableFuture.failedFuture(throwable);
        }
    }

    private LlmProviderUnavailableException circuitOpenFailure(
            LlmProviderCircuitOpenState state
    ) {
        Duration retryAfter = Duration.between(
                clock.instant(),
                state.openUntil()
        );

        if (retryAfter.isNegative()) {
            retryAfter = Duration.ZERO;
        }

        log.warn(
                "LLM provider call skipped because circuit is open: "
                        + "provider={}, failureCategory={}, retryAfterSeconds={}",
                state.providerId(),
                state.failureCategory(),
                retryAfter.toSeconds()
        );

        return new LlmProviderUnavailableException(
                state.providerId(),
                state.failureCategory(),
                "LLM provider circuit is open: provider="
                        + state.providerId()
                        + ", failureCategory="
                        + state.failureCategory()
                        + ", retryAfterSeconds="
                        + retryAfter.toSeconds(),
                retryAfter
        );
    }

    private void recordProviderFailure(Throwable throwable) {
        Throwable cause = unwrap(throwable);

        if (cause instanceof LlmProviderUnavailableException failure
                && providerId().equalsIgnoreCase(failure.providerId())) {

            circuitBreaker.recordFailure(failure);
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {

            current = current.getCause();
        }

        return current;
    }
}