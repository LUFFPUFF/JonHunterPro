package ru.jobhunter.infrastructure.llm.openrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;
import ru.jobhunter.core.application.port.out.llm.LlmResponseFormat;
import ru.jobhunter.core.application.port.out.llm.LlmUsage;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatMessage;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatRequest;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChatResponse;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterChoice;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterResponseFormat;
import ru.jobhunter.infrastructure.llm.openrouter.dto.OpenRouterUsage;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProvider;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitBreaker;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitOpenState;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * OpenRouter adapter with ordered model fallback and per-model rate-limit
 * cooldowns.
 */
public class OpenRouterLlmAdapter implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(
            OpenRouterLlmAdapter.class
    );

    private static final String PROVIDER = "openrouter";

    private final OpenRouterProperties properties;
    private final OpenRouterLlmClient client;
    private final LlmProviderCircuitBreaker circuitBreaker;
    private final Clock clock;

    public OpenRouterLlmAdapter(
            OpenRouterProperties properties,
            OpenRouterLlmClient client,
            LlmProviderCircuitBreaker circuitBreaker,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "OpenRouter properties must not be null"
        );
        this.client = Objects.requireNonNull(
                client,
                "OpenRouter client must not be null"
        );
        this.circuitBreaker = Objects.requireNonNull(
                circuitBreaker,
                "LLM provider circuit breaker must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "Clock must not be null"
        );
    }

    @Override
    public String providerId() {
        return PROVIDER;
    }

    @Override
    public CompletableFuture<LlmGenerationResponse> generate(
            LlmGenerationRequest request
    ) {
        Objects.requireNonNull(
                request,
                "LLM generation request must not be null"
        );

        return tryGenerateWithModel(
                request,
                resolveModels(),
                0,
                null
        );
    }

    private CompletableFuture<LlmGenerationResponse> tryGenerateWithModel(
            LlmGenerationRequest request,
            List<String> models,
            int modelIndex,
            Throwable lastFailure
    ) {
        if (modelIndex >= models.size()) {
            return CompletableFuture.failedFuture(
                    modelsExhausted(models, lastFailure)
            );
        }

        String model = models.get(modelIndex);
        String circuitKey = OpenRouterCircuitKey.forModel(model);

        Optional<LlmProviderCircuitOpenState> openState =
                circuitBreaker.openState(circuitKey);

        if (openState.isPresent()) {
            return skipOpenCircuit(
                    request,
                    models,
                    modelIndex,
                    model,
                    openState.get()
            );
        }

        log.debug(
                "Calling OpenRouter model: useCase={}, model={}",
                request.useCase(),
                model
        );

        OpenRouterChatRequest chatRequest = new OpenRouterChatRequest(
                model,
                mapMessages(request.messages()),
                request.options().temperature(),
                request.options().maxTokens(),
                mapResponseFormat(request.options().responseFormat())
        );

        return client.createChatCompletion(chatRequest)
                .thenApply(response -> {
                    LlmGenerationResponse mappedResponse = mapResponse(
                            request.useCase(),
                            model,
                            response
                    );

                    circuitBreaker.recordSuccess(circuitKey);

                    return mappedResponse;
                })
                .exceptionallyCompose(exception ->
                        handleGenerationFailure(
                                request,
                                models,
                                modelIndex,
                                unwrap(exception)
                        )
                );
    }

    private CompletableFuture<LlmGenerationResponse> skipOpenCircuit(
            LlmGenerationRequest request,
            List<String> models,
            int modelIndex,
            String model,
            LlmProviderCircuitOpenState state
    ) {
        Duration retryAfter = Duration.between(
                clock.instant(),
                state.openUntil()
        );

        if (retryAfter.isNegative()) {
            retryAfter = Duration.ZERO;
        }

        log.warn(
                "OpenRouter model skipped because its circuit is open: "
                        + "useCase={}, model={}, failureCategory={}, "
                        + "retryAfterSeconds={}",
                request.useCase(),
                model,
                state.failureCategory(),
                retryAfter.toSeconds()
        );

        LlmProviderUnavailableException skippedModelFailure =
                new LlmProviderUnavailableException(
                        OpenRouterCircuitKey.forModel(model),
                        state.failureCategory(),
                        "OpenRouter model circuit is open: model="
                                + model
                                + ", retryAfterSeconds="
                                + retryAfter.toSeconds(),
                        retryAfter
                );

        return tryGenerateWithModel(
                request,
                models,
                modelIndex + 1,
                skippedModelFailure
        );
    }

    private CompletableFuture<LlmGenerationResponse> handleGenerationFailure(
            LlmGenerationRequest request,
            List<String> models,
            int modelIndex,
            Throwable failure
    ) {
        String failedModel = models.get(modelIndex);

        if (failure instanceof OpenRouterRateLimitException rateLimitException) {
            circuitBreaker.recordFailure(rateLimitException);

            log.warn(
                    "OpenRouter model rate limited; trying the next model "
                            + "without local retry: useCase={}, model={}, "
                            + "retryAfterSeconds={}, reason={}",
                    request.useCase(),
                    failedModel,
                    rateLimitException.retryAfter().toSeconds(),
                    rateLimitException.getMessage()
            );
        } else {
            log.warn(
                    "OpenRouter model failed; trying fallback model: "
                            + "useCase={}, failedModel={}, reason={}",
                    request.useCase(),
                    failedModel,
                    rootMessage(failure)
            );
        }

        return tryGenerateWithModel(
                request,
                models,
                modelIndex + 1,
                failure
        );
    }

    private OpenRouterModelsExhaustedException modelsExhausted(
            List<String> models,
            Throwable lastFailure
    ) {
        Throwable cause = lastFailure == null
                ? new OpenRouterLlmException(
                LlmFailureCategory.NETWORK_UNAVAILABLE,
                "No OpenRouter model was available for generation"
        )
                : lastFailure;

        return new OpenRouterModelsExhaustedException(
                models,
                cause
        );
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {

            current = current.getCause();
        }

        return current;
    }

    private LlmGenerationResponse mapResponse(
            String useCase,
            String requestedModel,
            OpenRouterChatResponse response
    ) {
        if (response == null) {
            throw new OpenRouterEmptyContentException(
                    requestedModel,
                    "response is empty"
            );
        }

        if (response.choices() == null
                || response.choices().isEmpty()) {

            log.warn(
                    "OpenRouter response has no choices: "
                            + "useCase={}, requestedModel={}, "
                            + "actualModel={}, responseId={}",
                    useCase,
                    requestedModel,
                    valueOrDefault(response.model(), requestedModel),
                    valueOrDefault(response.id(), "unknown")
            );

            throw new OpenRouterEmptyContentException(
                    requestedModel,
                    "response has no choices"
            );
        }

        OpenRouterChoice firstChoice = response.choices().getFirst();

        String actualModel = valueOrDefault(
                response.model(),
                requestedModel
        );

        String finishReason = firstChoice == null
                ? "missing-choice"
                : valueOrDefault(
                firstChoice.finishReason(),
                "unknown"
        );

        String content = firstChoice == null
                || firstChoice.message() == null
                ? ""
                : valueOrDefault(
                firstChoice.message().content(),
                ""
        );

        LlmUsage usage = mapUsage(response.usage());

        log.debug(
                "OpenRouter response received: "
                        + "useCase={}, requestedModel={}, actualModel={}, "
                        + "responseId={}, choicesCount={}, finishReason={}, "
                        + "contentLength={}, promptTokens={}, "
                        + "completionTokens={}, totalTokens={}",
                useCase,
                requestedModel,
                actualModel,
                valueOrDefault(response.id(), "unknown"),
                response.choices().size(),
                finishReason,
                content.length(),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens()
        );

        if (content.isBlank()) {
            throw new OpenRouterEmptyContentException(
                    requestedModel,
                    "response content is empty: actualModel="
                            + actualModel
                            + ", finishReason="
                            + finishReason
            );
        }

        return new LlmGenerationResponse(
                PROVIDER,
                actualModel,
                content,
                usage
        );
    }

    private OpenRouterResponseFormat mapResponseFormat(
            LlmResponseFormat responseFormat
    ) {
        return switch (responseFormat) {
            case TEXT -> null;
            case JSON_OBJECT -> OpenRouterResponseFormat.jsonObject();
        };
    }

    private LlmUsage mapUsage(OpenRouterUsage usage) {
        if (usage == null) {
            return LlmUsage.unknown();
        }

        return new LlmUsage(
                safeInt(usage.promptTokens()),
                safeInt(usage.completionTokens()),
                safeInt(usage.totalTokens())
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private List<OpenRouterChatMessage> mapMessages(
            List<LlmMessage> messages
    ) {
        return messages.stream()
                .map(message -> new OpenRouterChatMessage(
                        message.role().name()
                                .toLowerCase(Locale.ROOT),
                        message.content()
                ))
                .toList();
    }

    private List<String> resolveModels() {
        List<String> models = new ArrayList<>();

        models.add(properties.resolvedPrimaryModel());
        models.addAll(properties.resolvedFallbackModels());

        return models.stream()
                .map(String::trim)
                .filter(model -> !model.isBlank())
                .distinct()
                .toList();
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}