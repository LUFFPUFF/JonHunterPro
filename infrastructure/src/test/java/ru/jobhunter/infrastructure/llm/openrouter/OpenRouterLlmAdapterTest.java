package ru.jobhunter.infrastructure.llm.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitBreaker;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitOpenState;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenRouterLlmAdapterTest {

    private MockWebServer server;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.close();
    }

    @Test
    void shouldUseFallbackModelWhenPrimaryModelFails() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(500)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "error": {
                            "code": 500,
                            "message": "Primary model failed"
                          }
                        }
                        """)
                .build());

        server.enqueue(successfulResponse(
                "gen-fallback",
                "fallback/model",
                "Fallback generated cover letter"
        ));

        OpenRouterLlmAdapter adapter = createAdapter(
                "primary/model",
                "fallback/model"
        );

        LlmGenerationResponse response = adapter.generate(
                coverLetterRequest()
        ).join();

        assertEquals("openrouter", response.provider());
        assertEquals("fallback/model", response.model());
        assertEquals(
                "Fallback generated cover letter",
                response.content()
        );
        assertEquals(0, response.usage().promptTokens());
        assertEquals(0, response.usage().completionTokens());
        assertEquals(0, response.usage().totalTokens());

        RecordedRequest firstRequest = server.takeRequest();
        RecordedRequest secondRequest = server.takeRequest();

        assertEquals(
                "primary/model",
                requestModel(firstRequest)
        );
        assertEquals(
                "fallback/model",
                requestModel(secondRequest)
        );
    }

    @Test
    void shouldUseFallbackModelWhenPrimaryModelReturnsEmptyContent()
            throws Exception {

        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "gen-primary-empty",
                          "model": "primary/model",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "   "
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """)
                .build());

        server.enqueue(successfulResponse(
                "gen-fallback",
                "fallback/model",
                "Fallback generated cover letter"
        ));

        OpenRouterLlmAdapter adapter = createAdapter(
                "primary/model",
                "fallback/model"
        );

        LlmGenerationResponse response = adapter.generate(
                coverLetterRequest()
        ).join();

        assertEquals("openrouter", response.provider());
        assertEquals("fallback/model", response.model());
        assertEquals(
                "Fallback generated cover letter",
                response.content()
        );

        RecordedRequest firstRequest = server.takeRequest();
        RecordedRequest secondRequest = server.takeRequest();

        assertEquals(
                "primary/model",
                requestModel(firstRequest)
        );
        assertEquals(
                "fallback/model",
                requestModel(secondRequest)
        );
    }

    @Test
    void shouldFailWithModelsExhaustedExceptionWhenAllModelsReturnEmptyContent()
            throws Exception {

        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "gen-primary-empty",
                          "model": "primary/model",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": ""
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """)
                .build());

        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "gen-fallback-empty",
                          "model": "fallback/model",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": " "
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """)
                .build());

        OpenRouterLlmAdapter adapter = createAdapter(
                "primary/model",
                "fallback/model"
        );

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> adapter.generate(coverLetterRequest()).join()
        );

        OpenRouterModelsExhaustedException exhaustedException =
                assertInstanceOf(
                        OpenRouterModelsExhaustedException.class,
                        exception.getCause()
                );

        assertEquals(
                LlmFailureCategory.OPENROUTER_MODELS_EXHAUSTED,
                exhaustedException.failureCategory()
        );

        OpenRouterEmptyContentException emptyContentException =
                assertInstanceOf(
                        OpenRouterEmptyContentException.class,
                        exhaustedException.getCause()
                );

        assertEquals(
                LlmFailureCategory.OPENROUTER_EMPTY_CONTENT,
                emptyContentException.failureCategory()
        );

        RecordedRequest firstRequest = server.takeRequest();
        RecordedRequest secondRequest = server.takeRequest();

        assertEquals(
                "primary/model",
                requestModel(firstRequest)
        );
        assertEquals(
                "fallback/model",
                requestModel(secondRequest)
        );
    }

    @Test
    void shouldMapCoreMessagesToOpenRouterMessages() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "gen-primary",
                          "model": "primary/model",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "Generated text"
                              },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 1,
                            "completion_tokens": 2,
                            "total_tokens": 3
                          }
                        }
                        """)
                .build());

        OpenRouterLlmAdapter adapter = createAdapter(
                "primary/model",
                ""
        );

        LlmGenerationRequest request = new LlmGenerationRequest(
                "generate-cover-letter",
                List.of(
                        LlmMessage.system("System prompt"),
                        LlmMessage.user("User prompt"),
                        LlmMessage.assistant("Assistant example")
                ),
                new LlmGenerationOptions(0.2, 300)
        );

        adapter.generate(request).join();

        RecordedRequest recordedRequest = server.takeRequest();
        JsonNode body = objectMapper.readTree(
                recordedRequest.getBody().utf8()
        );

        assertTrue(
                recordedRequest.getRequestLine().startsWith(
                        "POST /api/v1/chat/completions"
                )
        );

        assertEquals("primary/model", body.get("model").asText());
        assertEquals(0.2, body.get("temperature").asDouble());
        assertEquals(300, body.get("max_tokens").asInt());

        assertEquals(
                "system",
                body.get("messages").get(0).get("role").asText()
        );
        assertEquals(
                "System prompt",
                body.get("messages").get(0).get("content").asText()
        );

        assertEquals(
                "user",
                body.get("messages").get(1).get("role").asText()
        );
        assertEquals(
                "User prompt",
                body.get("messages").get(1).get("content").asText()
        );

        assertEquals(
                "assistant",
                body.get("messages").get(2).get("role").asText()
        );
        assertEquals(
                "Assistant example",
                body.get("messages").get(2).get("content").asText()
        );
    }

    @Test
    void shouldSkipRateLimitedModelOnFollowingGenerationRequest()
            throws Exception {

        Clock clock = Clock.fixed(
                Instant.parse("2026-06-28T12:00:00Z"),
                ZoneOffset.UTC
        );

        TestCircuitBreaker circuitBreaker =
                new TestCircuitBreaker(clock);

        server.enqueue(new MockResponse.Builder()
                .code(429)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "120")
                .body("""
                        {
                          "error": {
                            "code": 429,
                            "message": "Rate limit exceeded: free-models-per-day"
                          }
                        }
                        """)
                .build());

        server.enqueue(successfulResponse(
                "fallback-first",
                "fallback/model",
                "Первое сопроводительное письмо"
        ));

        server.enqueue(successfulResponse(
                "fallback-second",
                "fallback/model",
                "Второе сопроводительное письмо"
        ));

        OpenRouterLlmAdapter adapter = createAdapter(
                "primary/model",
                "fallback/model",
                circuitBreaker,
                clock
        );

        adapter.generate(coverLetterRequest()).join();
        adapter.generate(coverLetterRequest()).join();

        RecordedRequest firstRequest = server.takeRequest();
        RecordedRequest secondRequest = server.takeRequest();
        RecordedRequest thirdRequest = server.takeRequest();

        assertEquals("primary/model", requestModel(firstRequest));
        assertEquals("fallback/model", requestModel(secondRequest));
        assertEquals("fallback/model", requestModel(thirdRequest));

        assertTrue(
                circuitBreaker.openState(
                        OpenRouterCircuitKey.forModel("primary/model")
                ).isPresent()
        );
    }

    private LlmGenerationRequest coverLetterRequest() {
        return new LlmGenerationRequest(
                "generate-cover-letter",
                List.of(
                        LlmMessage.system("You are a career assistant."),
                        LlmMessage.user("Generate cover letter.")
                ),
                new LlmGenerationOptions(0.7, 512)
        );
    }

    private String requestModel(RecordedRequest request) throws Exception {
        JsonNode body = objectMapper.readTree(
                request.getBody().utf8()
        );

        return body.get("model").asText();
    }

    private MockResponse successfulResponse(
            String responseId,
            String model,
            String content
    ) {
        return new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "%s",
                          "model": "%s",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "%s"
                              },
                              "finish_reason": "stop"
                            }
                          ]
                        }
                        """.formatted(responseId, model, content))
                .build();
    }

    private OpenRouterLlmAdapter createAdapter(
            String primaryModel,
            String fallbackModels
    ) {
        Clock clock = Clock.systemUTC();

        return createAdapter(
                primaryModel,
                fallbackModels,
                new TestCircuitBreaker(clock),
                clock
        );
    }

    private OpenRouterLlmAdapter createAdapter(
            String primaryModel,
            String fallbackModels,
            LlmProviderCircuitBreaker circuitBreaker,
            Clock clock
    ) {
        OpenRouterProperties properties = new OpenRouterProperties(
                true,
                server.url("/").toString(),
                "test-api-key",
                primaryModel,
                fallbackModels,
                "https://jobhunterpro.local",
                "JobHunterPro Test",
                5,
                5,
                5
        );

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.resolvedConnectTimeout())
                .readTimeout(properties.resolvedReadTimeout())
                .writeTimeout(properties.resolvedWriteTimeout())
                .build();

        OpenRouterLlmClient client = new OpenRouterLlmClient(
                properties,
                objectMapper,
                httpClient
        );

        return new OpenRouterLlmAdapter(
                properties,
                client,
                circuitBreaker,
                clock
        );
    }

    private static final class TestCircuitBreaker
            implements LlmProviderCircuitBreaker {

        private final Clock clock;
        private final Map<String, LlmProviderCircuitOpenState> states =
                new HashMap<>();

        private TestCircuitBreaker(Clock clock) {
            this.clock = clock;
        }

        @Override
        public Optional<LlmProviderCircuitOpenState> openState(
                String providerId
        ) {
            LlmProviderCircuitOpenState state = states.get(providerId);

            if (state == null) {
                return Optional.empty();
            }

            if (state.openUntil().isAfter(clock.instant())) {
                return Optional.of(state);
            }

            states.remove(providerId);

            return Optional.empty();
        }

        @Override
        public void recordSuccess(String providerId) {
            states.remove(providerId);
        }

        @Override
        public void recordFailure(
                LlmProviderUnavailableException failure
        ) {
            if (failure.failureCategory()
                    != LlmFailureCategory.OPENROUTER_RATE_LIMIT) {
                return;
            }

            Duration cooldown = failure.retryAfter().isZero()
                    ? Duration.ofMinutes(15)
                    : failure.retryAfter();

            states.put(
                    failure.providerId(),
                    new LlmProviderCircuitOpenState(
                            failure.providerId(),
                            failure.failureCategory(),
                            clock.instant().plus(cooldown)
                    )
            );
        }
    }
}
