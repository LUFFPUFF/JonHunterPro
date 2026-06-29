package ru.jobhunter.infrastructure.llm.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaLlmAdapterTest {

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
    void shouldDisableThinkingForCoverLetterGeneration()
            throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""
                        {
                          "model": "qwen3:4b",
                          "message": {
                            "role": "assistant",
                            "content": "Здравствуйте! Готовое письмо."
                          },
                          "done": true,
                          "prompt_eval_count": 10,
                          "eval_count": 20
                        }
                        """)
                .build());

        OllamaLlmAdapter adapter = createAdapter();

        adapter.generate(new LlmGenerationRequest(
                "generate-cover-letter",
                List.of(
                        LlmMessage.system("Return only a cover letter."),
                        LlmMessage.user("Generate a cover letter.")
                ),
                LlmGenerationOptions.coverLetter()
        )).join();

        RecordedRequest request = server.takeRequest();
        Assertions.assertNotNull(request.getBody());
        JsonNode body = objectMapper.readTree(
                request.getBody().utf8()
        );

        assertFalse(body.path("think").asBoolean(true));
        assertTrue(body.path("stream").isBoolean());
        assertFalse(body.path("stream").asBoolean());
    }

    private OllamaLlmAdapter createAdapter() {
        OllamaProperties properties = new OllamaProperties(
                true,
                server.url("/").toString(),
                "test-model",
                5,
                5,
                5,
                0,
                null
        );

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.resolvedConnectTimeout())
                .readTimeout(properties.resolvedReadTimeout())
                .writeTimeout(properties.resolvedWriteTimeout())
                .build();

        OllamaLlmClient client = new OllamaLlmClient(
                properties,
                objectMapper,
                httpClient
        );

        return new OllamaLlmAdapter(properties, client);
    }
}