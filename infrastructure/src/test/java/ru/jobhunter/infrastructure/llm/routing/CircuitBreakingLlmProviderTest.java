package ru.jobhunter.infrastructure.llm.routing;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitBreakingLlmProviderTest {

    @Test
    void shouldSkipNextOllamaCallAfterCudaRuntimeCrash() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-06-28T10:00:00Z")
        );

        InMemoryLlmProviderCircuitBreaker circuitBreaker =
                new InMemoryLlmProviderCircuitBreaker(
                        new LlmCircuitBreakerFailurePolicy(
                                new LlmCircuitBreakerProperties(900, 900)
                        ),
                        clock
                );

        FailingOllamaProvider delegate =
                new FailingOllamaProvider();

        CircuitBreakingLlmProvider provider =
                new CircuitBreakingLlmProvider(
                        delegate,
                        circuitBreaker,
                        clock
                );

        assertThrows(
                CompletionException.class,
                () -> provider.generate(request()).join()
        );

        CompletionException blockedException = assertThrows(
                CompletionException.class,
                () -> provider.generate(request()).join()
        );

        LlmProviderUnavailableException failure =
                (LlmProviderUnavailableException)
                        blockedException.getCause();

        assertEquals(
                LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                failure.failureCategory()
        );
        assertEquals(1, delegate.generateCalls());
    }

    @Test
    void shouldCallOllamaAgainAfterCooldownExpires() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-06-28T10:00:00Z")
        );

        InMemoryLlmProviderCircuitBreaker circuitBreaker =
                new InMemoryLlmProviderCircuitBreaker(
                        new LlmCircuitBreakerFailurePolicy(
                                new LlmCircuitBreakerProperties(900, 900)
                        ),
                        clock
                );

        FailingOllamaProvider delegate =
                new FailingOllamaProvider();

        CircuitBreakingLlmProvider provider =
                new CircuitBreakingLlmProvider(
                        delegate,
                        circuitBreaker,
                        clock
                );

        assertThrows(
                CompletionException.class,
                () -> provider.generate(request()).join()
        );

        assertThrows(
                CompletionException.class,
                () -> provider.generate(request()).join()
        );

        clock.advance(Duration.ofMinutes(15));

        assertThrows(
                CompletionException.class,
                () -> provider.generate(request()).join()
        );

        assertEquals(2, delegate.generateCalls());
    }

    private LlmGenerationRequest request() {
        return new LlmGenerationRequest(
                "generate-cover-letter",
                List.of(
                        LlmMessage.system(
                                "You are a career assistant."
                        ),
                        LlmMessage.user(
                                "Generate a concise cover letter."
                        )
                ),
                LlmGenerationOptions.coverLetter()
        );
    }

    private static final class FailingOllamaProvider
            implements LlmProvider {

        private int generateCalls;

        @Override
        public String providerId() {
            return "ollama";
        }

        @Override
        public CompletableFuture<LlmGenerationResponse> generate(
                LlmGenerationRequest request
        ) {
            generateCalls++;

            return CompletableFuture.failedFuture(
                    new LlmProviderUnavailableException(
                            "ollama",
                            LlmFailureCategory.OLLAMA_RUNTIME_CRASH,
                            "llama-server process has terminated: "
                                    + "CUDA error: unsupported PTX toolchain"
                    )
            );
        }

        private int generateCalls() {
            return generateCalls;
        }
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