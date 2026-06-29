package ru.jobhunter.infrastructure.llm.routing;

import org.junit.jupiter.api.Test;
import ru.jobhunter.infrastructure.llm.openrouter.OpenRouterCircuitKey;
import ru.jobhunter.infrastructure.llm.openrouter.OpenRouterModelsExhaustedException;
import ru.jobhunter.infrastructure.llm.openrouter.OpenRouterRateLimitException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLlmProviderCircuitBreakerTest {

    private static final Instant NOW =
            Instant.parse("2026-06-28T10:00:00Z");

    @Test
    void shouldOpenOllamaCircuitForRuntimeCrash() {
        MutableClock clock = new MutableClock(NOW);

        InMemoryLlmProviderCircuitBreaker circuitBreaker =
                createCircuitBreaker(clock);

        circuitBreaker.recordFailure(
                new LlmProviderUnavailableException(
                        "ollama",
                        LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                        "llama-server process has terminated: "
                                + "CUDA PTX unsupported toolchain"
                )
        );

        LlmProviderCircuitOpenState state =
                circuitBreaker.openState("ollama").orElseThrow();

        assertEquals(
                LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                state.failureCategory()
        );
        assertEquals(
                NOW.plus(Duration.ofMinutes(15)),
                state.openUntil()
        );
    }

    @Test
    void shouldUseRetryAfterFromNestedOpenRouterRateLimitFailure() {
        MutableClock clock = new MutableClock(NOW);

        InMemoryLlmProviderCircuitBreaker circuitBreaker =
                createCircuitBreaker(clock);

        OpenRouterRateLimitException rateLimitException =
                new OpenRouterRateLimitException(
                        "qwen/qwen3-next-80b-a3b-instruct:free",
                        "Rate limit exceeded: free-models-per-day",
                        Duration.ofSeconds(45)
                );

        circuitBreaker.recordFailure(
                new OpenRouterModelsExhaustedException(
                        List.of(
                                "qwen/qwen3-next-80b-a3b-instruct:free",
                                "openai/gpt-oss-20b:free"
                        ),
                        rateLimitException
                )
        );

        LlmProviderCircuitOpenState state =
                circuitBreaker.openState(
                        OpenRouterCircuitKey.forModel(
                                "qwen/qwen3-next-80b-a3b-instruct:free"
                        )
                ).orElseThrow();

        assertEquals(
                LlmFailureCategory.OPENROUTER_RATE_LIMIT,
                state.failureCategory()
        );
        assertEquals(
                NOW.plusSeconds(45),
                state.openUntil()
        );
    }

    @Test
    void shouldAllowProviderAfterCooldownExpires() {
        MutableClock clock = new MutableClock(NOW);

        InMemoryLlmProviderCircuitBreaker circuitBreaker =
                createCircuitBreaker(clock);

        circuitBreaker.recordFailure(
                new LlmProviderUnavailableException(
                        "ollama",
                        LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                        "Ollama runtime crash"
                )
        );

        assertTrue(circuitBreaker.openState("ollama").isPresent());

        clock.advance(Duration.ofMinutes(15));

        assertFalse(circuitBreaker.openState("ollama").isPresent());
    }

    @Test
    void shouldNotOpenCircuitForInvalidModelOutput() {
        MutableClock clock = new MutableClock(NOW);

        InMemoryLlmProviderCircuitBreaker circuitBreaker =
                createCircuitBreaker(clock);

        circuitBreaker.recordFailure(
                new LlmProviderUnavailableException(
                        "openrouter",
                        LlmFailureCategory.INVALID_MODEL_OUTPUT,
                        "OpenRouter response cannot be parsed"
                )
        );

        assertFalse(circuitBreaker.openState("openrouter").isPresent());
    }

    private InMemoryLlmProviderCircuitBreaker createCircuitBreaker(
            Clock clock
    ) {
        LlmCircuitBreakerProperties properties =
                new LlmCircuitBreakerProperties(
                        900,
                        900
                );

        return new InMemoryLlmProviderCircuitBreaker(
                new LlmCircuitBreakerFailurePolicy(properties),
                clock
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}