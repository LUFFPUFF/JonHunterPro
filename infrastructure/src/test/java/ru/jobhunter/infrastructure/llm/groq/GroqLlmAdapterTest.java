package ru.jobhunter.infrastructure.llm.groq;

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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqLlmAdapterTest {

    private MockWebServer server;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void shouldSendSingleGroqRequestAndMapResponse()
            throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "chatcmpl-groq-test",
                          "model": "openai/gpt-oss-120b",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "Сопроводительное письмо сформировано."
                              },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 10,
                            "completion_tokens": 20,
                            "total_tokens": 30
                          }
                        }
                        """)
                .build());

        GroqLlmAdapter adapter = createAdapter();

        LlmGenerationResponse response = adapter.generate(
                new LlmGenerationRequest(
                        "generate-cover-letter",
                        List.of(
                                LlmMessage.system(
                                        "You are a career assistant."
                                ),
                                LlmMessage.user(
                                        "Generate a cover letter."
                                )
                        ),
                        new LlmGenerationOptions(0.2, 300)
                )
        ).join();

        assertEquals("groq", response.provider());
        assertEquals("openai/gpt-oss-120b", response.model());
        assertEquals(
                "Сопроводительное письмо сформировано.",
                response.content()
        );
        assertEquals(10, response.usage().promptTokens());
        assertEquals(20, response.usage().completionTokens());
        assertEquals(30, response.usage().totalTokens());

        RecordedRequest request = server.takeRequest();

        assertTrue(
                request.getRequestLine().startsWith(
                        "POST /openai/v1/chat/completions"
                )
        );

        assertEquals(
                "Bearer test-groq-api-key",
                request.getHeaders().get("Authorization")
        );

        JsonNode body = objectMapper.readTree(
                request.getBody().utf8()
        );

        assertEquals(
                "openai/gpt-oss-120b",
                body.get("model").asText()
        );
        assertEquals(
                0.2,
                body.get("temperature").asDouble()
        );
        assertEquals(
                300,
                body.get("max_completion_tokens").asInt()
        );
        assertEquals(
                "low",
                body.get("reasoning_effort").asText()
        );
        assertEquals(
                "system",
                body.get("messages").get(0).get("role").asText()
        );
        assertEquals(
                "user",
                body.get("messages").get(1).get("role").asText()
        );
    }

    @Test
    void shouldExposeRateLimitWithoutTryingAnotherProvider()
            throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(429)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "17")
                .body("""
                        {
                          "error": {
                            "message": "Rate limit exceeded"
                          }
                        }
                        """)
                .build());

        GroqLlmAdapter adapter = createAdapter();

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> adapter.generate(
                        new LlmGenerationRequest(
                                "generate-cover-letter",
                                List.of(
                                        LlmMessage.user("Generate letter")
                                ),
                                new LlmGenerationOptions(0.2, 300)
                        )
                ).join()
        );

        GroqRateLimitException rateLimitException =
                assertInstanceOf(
                        GroqRateLimitException.class,
                        exception.getCause()
                );

        assertEquals(
                Duration.ofSeconds(17),
                rateLimitException.retryAfter()
        );

        RecordedRequest request = server.takeRequest();

        assertTrue(
                request.getRequestLine().startsWith(
                        "POST /openai/v1/chat/completions"
                )
        );
    }

    private GroqLlmAdapter createAdapter() {
        GroqProperties properties = new GroqProperties(
                true,
                server.url("/openai/v1").toString(),
                "test-groq-api-key",
                "openai/gpt-oss-120b",
                "low",
                5,
                5,
                5
        );

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(
                        properties.resolvedConnectTimeout()
                )
                .readTimeout(
                        properties.resolvedReadTimeout()
                )
                .writeTimeout(
                        properties.resolvedWriteTimeout()
                )
                .build();

        GroqLlmClient client = new GroqLlmClient(
                properties,
                objectMapper,
                httpClient
        );

        return new GroqLlmAdapter(
                properties,
                client
        );
    }
}