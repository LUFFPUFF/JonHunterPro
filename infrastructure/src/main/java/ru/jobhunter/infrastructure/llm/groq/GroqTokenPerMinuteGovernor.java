package ru.jobhunter.infrastructure.llm.groq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class GroqTokenPerMinuteGovernor {

    private static final Logger log = LoggerFactory.getLogger(
            GroqTokenPerMinuteGovernor.class
    );

    private static final int PROVIDER_TOKENS_PER_MINUTE = 8_000;

    private static final int SAFE_TOKENS_PER_MINUTE = 7_200;

    private static final long NANOS_PER_MINUTE =
            TimeUnit.MINUTES.toNanos(1);

    private static final long INPUT_TOKEN_NUMERATOR = 10L;
    private static final long INPUT_TOKEN_DENOMINATOR = 27L;

    private static final int REQUEST_OVERHEAD_TOKENS = 160;

    private final int tokensPerMinute;
    private final LongSupplier nanoTimeSource;
    private final Object monitor = new Object();

    private long theoreticalArrivalTimeNanos = Long.MIN_VALUE;

    public GroqTokenPerMinuteGovernor() {
        this(
                SAFE_TOKENS_PER_MINUTE,
                System::nanoTime
        );
    }

    GroqTokenPerMinuteGovernor(
            int tokensPerMinute,
            LongSupplier nanoTimeSource
    ) {
        if (tokensPerMinute < 1
                || tokensPerMinute > PROVIDER_TOKENS_PER_MINUTE) {
            throw new IllegalArgumentException(
                    "Groq TPM budget must be between 1 and "
                            + PROVIDER_TOKENS_PER_MINUTE
            );
        }

        this.tokensPerMinute = tokensPerMinute;
        this.nanoTimeSource = Objects.requireNonNull(
                nanoTimeSource,
                "Nano time source must not be null"
        );
    }

    public CompletableFuture<Void> awaitBudget(
            LlmGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "LLM generation request must not be null"
        );

        int estimatedTokens = estimateRequestedTokens(request);
        Duration delay = reserveTokenBudget(estimatedTokens);

        if (delay.isZero()) {
            return CompletableFuture.completedFuture(null);
        }

        log.info(
                "Groq TPM governor delayed request: useCase={}, "
                        + "estimatedTokens={}, delayMillis={}, "
                        + "safeTpmBudget={}",
                request.useCase(),
                estimatedTokens,
                delay.toMillis(),
                tokensPerMinute
        );

        return CompletableFuture.runAsync(
                () -> {
                },
                CompletableFuture.delayedExecutor(
                        delay.toNanos(),
                        TimeUnit.NANOSECONDS
                )
        );
    }

    int estimateRequestedTokens(
            LlmGenerationRequest request
    ) {
        long messageCharacters = request.messages()
                .stream()
                .mapToLong(message -> message.content().length())
                .sum();

        long estimatedInputTokens = ceilDivide(
                Math.multiplyExact(
                        messageCharacters,
                        INPUT_TOKEN_NUMERATOR
                ),
                INPUT_TOKEN_DENOMINATOR
        );

        long total = estimatedInputTokens
                + request.options().maxTokens()
                + REQUEST_OVERHEAD_TOKENS;

        if (total > tokensPerMinute) {
            throw new IllegalArgumentException(
                    "Groq request exceeds safe TPM budget: estimatedTokens="
                            + total
                            + ", safeTpmBudget="
                            + tokensPerMinute
                            + ", useCase="
                            + request.useCase()
            );
        }

        return Math.toIntExact(total);
    }

    Duration reserveTokenBudget(
            int requestedTokens
    ) {
        if (requestedTokens < 1
                || requestedTokens > tokensPerMinute) {
            throw new IllegalArgumentException(
                    "Requested Groq token budget must be between 1 and "
                            + tokensPerMinute
            );
        }

        synchronized (monitor) {
            long now = nanoTimeSource.getAsLong();

            long currentArrivalTime =
                    theoreticalArrivalTimeNanos == Long.MIN_VALUE
                            ? now
                            : Math.max(
                            now,
                            theoreticalArrivalTimeNanos
                    );

            long remainingBurstTokens =
                    tokensPerMinute - requestedTokens;

            long burstAllowanceNanos = nanosForTokens(
                    (int) remainingBurstTokens
            );

            long permittedAt = Math.max(
                    now,
                    subtractSaturating(
                            currentArrivalTime,
                            burstAllowanceNanos
                    )
            );

            theoreticalArrivalTimeNanos = addSaturating(
                    currentArrivalTime,
                    nanosForTokens(requestedTokens)
            );

            return Duration.ofNanos(
                    Math.max(0L, permittedAt - now)
            );
        }
    }

    private long nanosForTokens(
            int tokens
    ) {
        if (tokens == 0) {
            return 0L;
        }

        return ceilDivide(
                Math.multiplyExact(
                        (long) tokens,
                        NANOS_PER_MINUTE
                ),
                tokensPerMinute
        );
    }

    private long ceilDivide(
            long dividend,
            long divisor
    ) {
        return (dividend + divisor - 1L) / divisor;
    }

    private long subtractSaturating(
            long value,
            long amount
    ) {
        if (amount > 0L
                && value < Long.MIN_VALUE + amount) {
            return Long.MIN_VALUE;
        }

        return value - amount;
    }

    private long addSaturating(
            long value,
            long amount
    ) {
        if (amount > 0L
                && value > Long.MAX_VALUE - amount) {
            return Long.MAX_VALUE;
        }

        return value + amount;
    }
}