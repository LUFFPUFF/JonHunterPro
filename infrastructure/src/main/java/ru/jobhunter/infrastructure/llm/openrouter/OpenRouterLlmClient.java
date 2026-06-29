package ru.jobhunter.infrastructure.llm.openrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatRequest;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatResponse;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterErrorResponse;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class OpenRouterLlmClient {

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public OpenRouterLlmClient(
            OpenRouterProperties properties,
            ObjectMapper objectMapper,
            OkHttpClient httpClient
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "OpenRouter properties must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "ObjectMapper must not be null"
        );
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "OkHttp client must not be null"
        );
    }

    public CompletableFuture<OpenRouterChatResponse> createChatCompletion(
            OpenRouterChatRequest chatRequest
    ) {
        Objects.requireNonNull(
                chatRequest,
                "OpenRouter chat request must not be null"
        );

        CompletableFuture<OpenRouterChatResponse> future =
                new CompletableFuture<>();

        try {
            String jsonBody = objectMapper.writeValueAsString(chatRequest);

            Request request = new Request.Builder()
                    .url(chatCompletionsUrl())
                    .post(RequestBody.create(jsonBody, JSON))
                    .header(
                            "Authorization",
                            "Bearer " + properties.apiKey().trim()
                    )
                    .header("Content-Type", "application/json")
                    .header(
                            "HTTP-Referer",
                            safeHeaderValue(properties.httpReferer())
                    )
                    .header(
                            "X-OpenRouter-Title",
                            safeHeaderValue(properties.applicationTitle())
                    )
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(
                        Call call,
                        IOException exception
                ) {
                    future.completeExceptionally(
                            new OpenRouterLlmException(
                                    LlmFailureCategory.NETWORK_UNAVAILABLE,
                                    "OpenRouter request failed",
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
                            completeHttpFailure(
                                    future,
                                    response,
                                    responseBody,
                                    chatRequest.model()
                            );
                            return;
                        }

                        OpenRouterChatResponse chatResponse =
                                objectMapper.readValue(
                                        responseBody,
                                        OpenRouterChatResponse.class
                                );

                        future.complete(chatResponse);
                    } catch (Exception exception) {
                        future.completeExceptionally(
                                new OpenRouterLlmException(
                                        LlmFailureCategory.INVALID_MODEL_OUTPUT,
                                        "Failed to parse OpenRouter response",
                                        exception
                                )
                        );
                    }
                }
            });
        } catch (Exception exception) {
            future.completeExceptionally(
                    new OpenRouterLlmException(
                            LlmFailureCategory.NETWORK_UNAVAILABLE,
                            "Failed to build OpenRouter request",
                            exception
                    )
            );
        }

        return future;
    }

    private void completeHttpFailure(
            CompletableFuture<OpenRouterChatResponse> future,
            Response response,
            String responseBody,
            String requestedModel
    ) {
        String errorMessage = buildErrorMessage(
                response.code(),
                responseBody
        );

        if (response.code() == 429) {
            future.completeExceptionally(
                    new OpenRouterRateLimitException(
                            requestedModel,
                            errorMessage,
                            parseRetryAfter(
                                    response.header("Retry-After")
                            )
                    )
            );
            return;
        }

        future.completeExceptionally(
                new OpenRouterLlmException(
                        LlmFailureCategory.NETWORK_UNAVAILABLE,
                        errorMessage
                )
        );
    }

    private String chatCompletionsUrl() {
        HttpUrl baseUrl = HttpUrl.parse(properties.resolvedBaseUrl());

        if (baseUrl == null) {
            throw new OpenRouterConfigurationException(
                    "Invalid OpenRouter base URL: "
                            + properties.resolvedBaseUrl()
            );
        }

        return baseUrl.newBuilder()
                .addPathSegments("api/v1/chat/completions")
                .build()
                .toString();
    }

    private String buildErrorMessage(
            int statusCode,
            String responseBody
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            return "OpenRouter request failed with HTTP status "
                    + statusCode;
        }

        try {
            OpenRouterErrorResponse errorResponse =
                    objectMapper.readValue(
                            responseBody,
                            OpenRouterErrorResponse.class
                    );

            if (errorResponse.error() != null
                    && errorResponse.error().message() != null) {
                return "OpenRouter request failed with HTTP status "
                        + statusCode
                        + ": "
                        + errorResponse.error().message();
            }
        } catch (Exception ignored) {
            // Используется raw-body fallback ниже.
        }

        return "OpenRouter request failed with HTTP status "
                + statusCode
                + ": "
                + responseBody;
    }

    private Duration parseRetryAfter(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return Duration.ZERO;
        }

        try {
            long seconds = Long.parseLong(retryAfterHeader.trim());

            if (seconds <= 0) {
                return Duration.ZERO;
            }

            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            return Duration.ZERO;
        }
    }

    private String safeHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return "JobHunterPro";
        }

        return value.trim();
    }
}