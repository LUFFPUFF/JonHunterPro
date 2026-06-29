package ru.jobhunter.infrastructure.llm.groq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;
import ru.jobhunter.core.application.port.out.llm.LlmResponseFormat;
import ru.jobhunter.core.application.port.out.llm.LlmUsage;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProvider;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GroqLlmAdapter implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(
            GroqLlmAdapter.class
    );

    private final GroqProperties properties;
    private final GroqLlmClient client;
    private final GroqTokenPerMinuteGovernor tokenGovernor;

    public GroqLlmAdapter(
            GroqProperties properties,
            GroqLlmClient client
    ) {
        this(
                properties,
                client,
                new GroqTokenPerMinuteGovernor()
        );
    }

    public GroqLlmAdapter(
            GroqProperties properties,
            GroqLlmClient client,
            GroqTokenPerMinuteGovernor tokenGovernor
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Groq properties must not be null"
        );
        this.client = Objects.requireNonNull(
                client,
                "Groq client must not be null"
        );
        this.tokenGovernor = Objects.requireNonNull(
                tokenGovernor,
                "Groq token governor must not be null"
        );
    }

    @Override
    public String providerId() {
        return GroqLlmException.PROVIDER_ID;
    }

    @Override
    public CompletableFuture<LlmGenerationResponse> generate(
            LlmGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "LLM generation request must not be null"
        );

        GroqChatRequest chatRequest = new GroqChatRequest(
                properties.resolvedModel(),
                mapMessages(request.messages()),
                request.options().temperature(),
                request.options().maxTokens(),
                mapResponseFormat(
                        request.options().responseFormat()
                ),
                properties.resolvedReasoningEffort()
        );

        int estimatedTokens = tokenGovernor.estimateRequestedTokens(
                request
        );

        return tokenGovernor.awaitBudget(request)
                .thenCompose(ignored -> {
                    log.debug(
                            "Calling Groq model: useCase={}, model={}, "
                                    + "responseFormat={}, reasoningEffort={}, "
                                    + "estimatedTokens={}",
                            request.useCase(),
                            chatRequest.model(),
                            request.options().responseFormat(),
                            chatRequest.reasoningEffort(),
                            estimatedTokens
                    );

                    return client.createChatCompletion(chatRequest);
                })
                .thenApply(response -> mapResponse(
                        request.useCase(),
                        chatRequest.model(),
                        response
                ));
    }

    private LlmGenerationResponse mapResponse(
            String useCase,
            String requestedModel,
            GroqChatResponse response
    ) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {
            throw new GroqLlmException(
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Groq response has no choices"
            );
        }

        GroqChoice firstChoice = response.choices().getFirst();

        String actualModel = normalizeOrDefault(
                response.model(),
                requestedModel
        );

        String finishReason = firstChoice == null
                ? "missing-choice"
                : normalizeOrDefault(
                firstChoice.finishReason(),
                "unknown"
        );

        String content = firstChoice == null
                || firstChoice.message() == null
                ? ""
                : normalizeOrDefault(
                firstChoice.message().content(),
                ""
        );

        LlmUsage usage = mapUsage(response.usage());

        log.debug(
                "Groq response received: useCase={}, requestedModel={}, "
                        + "actualModel={}, responseId={}, finishReason={}, "
                        + "contentLength={}, promptTokens={}, "
                        + "completionTokens={}, totalTokens={}",
                useCase,
                requestedModel,
                actualModel,
                normalizeOrDefault(response.id(), "unknown"),
                finishReason,
                content.length(),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens()
        );

        if (content.isBlank()) {
            throw new GroqLlmException(
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Groq response content is empty: actualModel="
                            + actualModel
                            + ", finishReason="
                            + finishReason
            );
        }

        return new LlmGenerationResponse(
                providerId(),
                actualModel,
                content,
                usage
        );
    }

    private List<GroqChatMessage> mapMessages(
            List<LlmMessage> messages
    ) {
        return messages.stream()
                .map(message -> new GroqChatMessage(
                        message.role().name()
                                .toLowerCase(Locale.ROOT),
                        message.content()
                ))
                .toList();
    }

    private GroqResponseFormat mapResponseFormat(
            LlmResponseFormat responseFormat
    ) {
        return switch (responseFormat) {
            case TEXT -> null;
            case JSON_OBJECT -> GroqResponseFormat.jsonObject();
        };
    }

    private LlmUsage mapUsage(
            GroqUsage usage
    ) {
        if (usage == null) {
            return LlmUsage.unknown();
        }

        return new LlmUsage(
                safeInt(usage.promptTokens()),
                safeInt(usage.completionTokens()),
                safeInt(usage.totalTokens())
        );
    }

    private int safeInt(
            Integer value
    ) {
        return value == null
                ? 0
                : Math.max(value, 0);
    }

    private String normalizeOrDefault(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }
}