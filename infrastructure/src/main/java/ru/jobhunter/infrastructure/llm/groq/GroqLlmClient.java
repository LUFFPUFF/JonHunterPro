package ru.jobhunter.infrastructure.llm.groq;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GroqLlmClient {

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final GroqProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public GroqLlmClient(
            GroqProperties properties,
            ObjectMapper objectMapper,
            OkHttpClient httpClient
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Groq properties must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "ObjectMapper must not be null"
        );
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "Groq HTTP client must not be null"
        );
    }

    public CompletableFuture<GroqChatResponse> createChatCompletion(
            GroqChatRequest chatRequest
    ) {
        Objects.requireNonNull(
                chatRequest,
                "Groq chat request must not be null"
        );

        CompletableFuture<GroqChatResponse> future =
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
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(
                        Call call,
                        IOException exception
                ) {
                    future.completeExceptionally(
                            new GroqLlmException(
                                    LlmFailureCategory.NETWORK_UNAVAILABLE,
                                    "Groq request failed",
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
                                    responseBody
                            );
                            return;
                        }

                        GroqChatResponse chatResponse =
                                objectMapper.readValue(
                                        responseBody,
                                        GroqChatResponse.class
                                );

                        future.complete(chatResponse);
                    } catch (Exception exception) {
                        future.completeExceptionally(
                                new GroqLlmException(
                                        LlmFailureCategory.INVALID_MODEL_OUTPUT,
                                        "Failed to parse Groq response",
                                        exception
                                )
                        );
                    }
                }
            });
        } catch (Exception exception) {
            future.completeExceptionally(
                    new GroqLlmException(
                            LlmFailureCategory.NETWORK_UNAVAILABLE,
                            "Failed to build Groq request",
                            exception
                    )
            );
        }

        return future;
    }

    private void completeHttpFailure(
            CompletableFuture<GroqChatResponse> future,
            Response response,
            String responseBody
    ) {
        String errorMessage = buildErrorMessage(
                response.code(),
                responseBody
        );

        if (response.code() == 429) {
            future.completeExceptionally(
                    new GroqRateLimitException(
                            errorMessage,
                            parseRetryAfter(
                                    response.header("Retry-After")
                            )
                    )
            );
            return;
        }

        future.completeExceptionally(
                new GroqLlmException(
                        LlmFailureCategory.NETWORK_UNAVAILABLE,
                        errorMessage
                )
        );
    }

    private String chatCompletionsUrl() {
        HttpUrl baseUrl = HttpUrl.parse(
                properties.resolvedBaseUrl()
        );

        if (baseUrl == null) {
            throw new GroqConfigurationException(
                    "Invalid Groq base URL: "
                            + properties.resolvedBaseUrl()
            );
        }

        return baseUrl.newBuilder()
                .addPathSegment("chat")
                .addPathSegment("completions")
                .build()
                .toString();
    }

    private String buildErrorMessage(
            int statusCode,
            String responseBody
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Groq request failed with HTTP status "
                    + statusCode;
        }

        try {
            GroqErrorResponse errorResponse =
                    objectMapper.readValue(
                            responseBody,
                            GroqErrorResponse.class
                    );

            if (errorResponse.error() != null
                    && errorResponse.error().message() != null
                    && !errorResponse.error().message().isBlank()) {
                return "Groq request failed with HTTP status "
                        + statusCode
                        + ": "
                        + errorResponse.error().message();
            }
        } catch (Exception ignored) {
            // Raw-body fallback is used below.
        }

        return "Groq request failed with HTTP status "
                + statusCode
                + ": "
                + responseBody;
    }

    private Duration parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ZERO;
        }

        try {
            long seconds = Long.parseLong(value.trim());

            return seconds > 0
                    ? Duration.ofSeconds(seconds)
                    : Duration.ZERO;
        } catch (NumberFormatException ignored) {
            return Duration.ZERO;
        }
    }
}