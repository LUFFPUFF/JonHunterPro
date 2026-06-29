package ru.jobhunter.infrastructure.llm.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.jobhunter.infrastructure.llm.ollama.dto.OllamaChatRequest;
import ru.jobhunter.infrastructure.llm.ollama.dto.OllamaChatResponse;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class OllamaLlmClient {

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public OllamaLlmClient(
            OllamaProperties properties,
            ObjectMapper objectMapper,
            OkHttpClient httpClient
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Ollama properties must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "ObjectMapper must not be null"
        );
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "Ollama HTTP client must not be null"
        );
    }

    public CompletableFuture<OllamaChatResponse> createChatCompletion(
            OllamaChatRequest chatRequest
    ) {
        Objects.requireNonNull(
                chatRequest,
                "Ollama chat request must not be null"
        );

        CompletableFuture<OllamaChatResponse> future =
                new CompletableFuture<>();

        try {
            String jsonBody = objectMapper.writeValueAsString(chatRequest);

            Request request = new Request.Builder()
                    .url(chatUrl())
                    .post(RequestBody.create(jsonBody, JSON))
                    .header("Content-Type", "application/json")
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(
                        Call call,
                        IOException exception
                ) {
                    future.completeExceptionally(
                            new OllamaLlmException(
                                    OllamaFailureCategoryResolver
                                            .fromTransportFailure(exception),
                                    "Ollama request failed",
                                    exception
                            )
                    );
                }

                @Override
                public void onResponse(
                        Call call,
                        Response response
                ) {
                    try (response) {
                        String responseBody = response.body() == null
                                ? ""
                                : response.body().string();

                        if (!response.isSuccessful()) {
                            future.completeExceptionally(
                                    new OllamaLlmException(
                                            OllamaFailureCategoryResolver.fromHttpFailure(
                                                    response.code(),
                                                    responseBody
                                            ),
                                            "Ollama request failed with HTTP status "
                                                    + response.code()
                                                    + ": "
                                                    + compact(responseBody)
                                    )
                            );
                            return;
                        }

                        future.complete(objectMapper.readValue(
                                responseBody,
                                OllamaChatResponse.class
                        ));
                    } catch (Exception exception) {
                        future.completeExceptionally(
                                new OllamaLlmException(
                                        LlmFailureCategory.INVALID_MODEL_OUTPUT,
                                        "Failed to parse Ollama response",
                                        exception
                                )
                        );
                    }
                }
            });
        } catch (Exception exception) {
            future.completeExceptionally(
                    new OllamaLlmException(
                            LlmFailureCategory.NETWORK_UNAVAILABLE,
                            "Failed to build Ollama request",
                            exception
                    )
            );
        }

        return future;
    }

    private String chatUrl() {
        HttpUrl baseUrl = HttpUrl.parse(properties.resolvedBaseUrl());

        if (baseUrl == null) {
            throw new OllamaConfigurationException(
                    "Invalid Ollama base URL: "
                            + properties.resolvedBaseUrl()
            );
        }

        return baseUrl.newBuilder()
                .addPathSegments("api/chat")
                .build()
                .toString();
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "No response body";
        }

        String compactValue = value.replaceAll("\\s+", " ").trim();

        return compactValue.length() <= 500
                ? compactValue
                : compactValue.substring(0, 499) + "…";
    }
}
