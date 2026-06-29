package ru.jobhunter.infrastructure.llm.openrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;

import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitBreaker;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitOpenState;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

import java.time.Clock;
import java.util.Optional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenRouterRealSmokeTest {

    private static final LlmProviderCircuitBreaker NO_OP_CIRCUIT_BREAKER =
            new LlmProviderCircuitBreaker() {

                @Override
                public Optional<LlmProviderCircuitOpenState> openState(
                        String providerId
                ) {
                    return Optional.empty();
                }

                @Override
                public void recordSuccess(String providerId) {
                }

                @Override
                public void recordFailure(
                        LlmProviderUnavailableException failure
                ) {
                }
            };

    @Test
    @EnabledIfEnvironmentVariable(
            named = "OPENROUTER_API_KEY",
            matches = ".+"
    )
    void shouldGenerateTextUsingRealOpenRouterApi() {
        String apiKey = System.getenv("OPENROUTER_API_KEY");

        OpenRouterProperties properties = new OpenRouterProperties(
                true,
                "https://openrouter.ai",
                apiKey,
                readEnvOrDefault("OPENROUTER_PRIMARY_MODEL", "mistralai/mistral-7b-instruct"),
                readEnvOrDefault("OPENROUTER_FALLBACK_MODELS", "google/gemma-7b-it"),
                readEnvOrDefault("OPENROUTER_HTTP_REFERER", "https://jobhunterpro.local"),
                readEnvOrDefault("OPENROUTER_APPLICATION_TITLE", "JobHunterPro"),
                30,
                90,
                30
        );

        properties.validateForEnabledProvider();

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.resolvedConnectTimeout())
                .readTimeout(properties.resolvedReadTimeout())
                .writeTimeout(properties.resolvedWriteTimeout())
                .build();

        OpenRouterLlmClient client = new OpenRouterLlmClient(
                properties,
                new ObjectMapper(),
                httpClient
        );

        OpenRouterLlmAdapter adapter = new OpenRouterLlmAdapter(
                properties,
                client,
                NO_OP_CIRCUIT_BREAKER,
                Clock.systemUTC()
        );

        LlmGenerationRequest request = new LlmGenerationRequest(
                "real-openrouter-smoke-test",
                List.of(
                        LlmMessage.system("""
                                Ты — ассистент для проверки LLM-интеграции.
                                Ответь строго одной короткой фразой на русском языке.
                                """),
                        LlmMessage.user("""
                                Напиши фразу о том, что интеграция OpenRouter в JobHunterPro работает.
                                """)
                ),
                new LlmGenerationOptions(0.2, 120)
        );

        LlmGenerationResponse response = adapter.generate(request).join();

        assertEquals("openrouter", response.provider());
        assertFalse(response.model().isBlank());
        assertFalse(response.content().isBlank());

        System.out.println("OpenRouter model: " + response.model());
        System.out.println("OpenRouter response: " + response.content());
    }

    private String readEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }
}