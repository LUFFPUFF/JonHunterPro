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
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatMessage;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatRequest;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatResponse;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterResponseFormat;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class OpenRouterLlmClientTest {

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
    void shouldSendChatCompletionRequestAndParseResponse() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "id": "gen-1",
                          "model": "test/model",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "Generated cover letter"
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

        OpenRouterLlmClient client = createClient();

        OpenRouterChatRequest request = new OpenRouterChatRequest(
                "test/model",
                List.of(
                        new OpenRouterChatMessage("system", "You are a career assistant."),
                        new OpenRouterChatMessage("user", "Generate cover letter.")
                ),
                0.7,
                512,
                OpenRouterResponseFormat.jsonObject()
        );

        OpenRouterChatResponse response = client.createChatCompletion(request).join();

        assertEquals("gen-1", response.id());
        assertEquals("test/model", response.model());
        assertEquals("Generated cover letter", response.choices().getFirst().message().content());
        assertEquals(10, response.usage().promptTokens());
        assertEquals(20, response.usage().completionTokens());
        assertEquals(30, response.usage().totalTokens());

        RecordedRequest recordedRequest = server.takeRequest();

        assertTrue(recordedRequest.getRequestLine().startsWith(
                "POST /api/v1/chat/completions"
        ));

        assertEquals("Bearer test-api-key", recordedRequest.getHeaders().get("Authorization"));

        String contentType = recordedRequest.getHeaders().get("Content-Type");
        assertNotNull(contentType);
        assertTrue(contentType.startsWith("application/json"));

        assertEquals("https://jobhunterpro.local", recordedRequest.getHeaders().get("HTTP-Referer"));
        assertEquals("JobHunterPro Test", recordedRequest.getHeaders().get("X-OpenRouter-Title"));

        JsonNode body = objectMapper.readTree(recordedRequest.getBody().utf8());

        assertEquals("test/model", body.get("model").asText());
        assertEquals(0.7, body.get("temperature").asDouble());
        assertEquals(512, body.get("max_tokens").asInt());

        assertEquals("system", body.get("messages").get(0).get("role").asText());
        assertEquals("You are a career assistant.", body.get("messages").get(0).get("content").asText());

        assertEquals("user", body.get("messages").get(1).get("role").asText());
        assertEquals("Generate cover letter.", body.get("messages").get(1).get("content").asText());
    }

    @Test
    void shouldFailWithReadableMessageWhenOpenRouterReturnsError() {
        server.enqueue(new MockResponse.Builder()
                .code(401)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "error": {
                            "code": 401,
                            "message": "Invalid API key"
                          }
                        }
                        """)
                .build());

        OpenRouterLlmClient client = createClient();

        OpenRouterChatRequest request = new OpenRouterChatRequest(
                "test/model",
                List.of(new OpenRouterChatMessage("user", "Hello")),
                0.7,
                512,
                OpenRouterResponseFormat.jsonObject()
        );

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> client.createChatCompletion(request).join()
        );

        assertInstanceOf(OpenRouterLlmException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("HTTP status 401"));
        assertTrue(exception.getCause().getMessage().contains("Invalid API key"));
    }

    @Test
    void shouldClassifyHttp429AsRateLimitFailure() {
        server.enqueue(new MockResponse.Builder()
                .code(429)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "12")
                .body("""
                    {
                      "error": {
                        "code": 429,
                        "message": "Rate limit exceeded: free-models-per-day"
                      }
                    }
                    """)
                .build());

        OpenRouterLlmClient client = createClient();

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> client.createChatCompletion(new OpenRouterChatRequest(
                        "test/model",
                        List.of(new OpenRouterChatMessage("user", "Hello")),
                        0.7,
                        512,
                        OpenRouterResponseFormat.jsonObject()
                )).join()
        );

        OpenRouterRateLimitException rateLimitException =
                assertInstanceOf(
                        OpenRouterRateLimitException.class,
                        exception.getCause()
                );

        assertEquals(
                LlmFailureCategory.OPENROUTER_RATE_LIMIT,
                rateLimitException.failureCategory()
        );
        assertEquals(
                "test/model",
                rateLimitException.model()
        );

        assertEquals(
                OpenRouterCircuitKey.forModel("test/model"),
                rateLimitException.providerId()
        );
        assertEquals(12, rateLimitException.retryAfter().toSeconds());
    }

    private OpenRouterLlmClient createClient() {
        OpenRouterProperties properties = new OpenRouterProperties(
                true,
                server.url("/").toString(),
                "test-api-key",
                "test/model",
                "",
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

        return new OpenRouterLlmClient(
                properties,
                objectMapper,
                httpClient
        );
    }
}