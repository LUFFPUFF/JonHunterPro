package ru.jobhunter.infrastructure.llm.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;
import ru.jobhunter.core.application.port.out.llm.LlmResponseFormat;
import ru.jobhunter.core.application.port.out.llm.LlmUsage;
import ru.jobhunter.infrastructure.llm.ollama.dto.OllamaChatMessage;
import ru.jobhunter.infrastructure.llm.ollama.dto.OllamaChatOptions;
import ru.jobhunter.infrastructure.llm.ollama.dto.OllamaChatRequest;
import ru.jobhunter.infrastructure.llm.ollama.dto.OllamaChatResponse;
import ru.jobhunter.infrastructure.llm.routing.LlmProvider;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OllamaLlmAdapter implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(
            OllamaLlmAdapter.class
    );

    private static final String PROVIDER = "ollama";

    private static final String QUESTIONNAIRE_CHOICE_USE_CASE =
            "answer-hh-questionnaire-choice";

    private static final String QUESTIONNAIRE_FORM_USE_CASE =
            "answer-hh-questionnaire-form";

    private final OllamaProperties properties;
    private final OllamaLlmClient client;

    public OllamaLlmAdapter(
            OllamaProperties properties,
            OllamaLlmClient client
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Ollama properties must not be null"
        );
        this.client = Objects.requireNonNull(
                client,
                "Ollama client must not be null"
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

        String model = properties.resolvedModel();
        long startedAtNanos = System.nanoTime();

        log.debug(
                "Calling Ollama model: useCase={}, model={}, "
                        + "responseFormat={}, think=false, contextLength={}",
                request.useCase(),
                model,
                request.options().responseFormat(),
                properties.resolvedContextLength()
        );

        OllamaChatRequest chatRequest = new OllamaChatRequest(
                model,
                mapMessages(request.messages()),
                false,
                mapResponseFormat(request),
                new OllamaChatOptions(
                        request.options().temperature(),
                        request.options().maxTokens(),
                        properties.resolvedContextLength()
                ),
                false,
                properties.resolvedKeepAlive()
        );

        return client.createChatCompletion(chatRequest)
                .thenApply(response ->
                        mapResponse(
                                request.useCase(),
                                model,
                                response,
                                TimeUnit.NANOSECONDS.toMillis(
                                        System.nanoTime() - startedAtNanos
                                )
                        )
                );
    }

    private LlmGenerationResponse mapResponse(
            String useCase,
            String requestedModel,
            OllamaChatResponse response,
            long latencyMs
    ) {
        if (response == null) {
            throw new OllamaLlmException(
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Ollama response is empty"
            );
        }

        if (!response.done()) {
            throw new OllamaLlmException(
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Ollama response is incomplete: useCase="
                            + useCase
                            + ", model="
                            + requestedModel
                            + ", doneReason="
                            + valueOrDefault(
                            response.doneReason(),
                            "unknown"
                    )
            );
        }

        if (response.message() == null) {
            throw new OllamaLlmException(
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Ollama response message is empty"
            );
        }

        String actualModel = valueOrDefault(
                response.model(),
                requestedModel
        );

        String content = trimToEmpty(
                response.message().content()
        );

        String thinking = trimToEmpty(
                response.message().thinking()
        );

        int promptTokens = safeInt(
                response.promptEvalCount()
        );

        int completionTokens = safeInt(
                response.evalCount()
        );

        log.debug(
                "Ollama response received: useCase={}, "
                        + "requestedModel={}, actualModel={}, "
                        + "doneReason={}, contentLength={}, "
                        + "thinkingLength={}, promptTokens={}, "
                        + "completionTokens={}, latencyMs={}, "
                        + "reportedTotalDurationMs={}",
                useCase,
                requestedModel,
                actualModel,
                valueOrDefault(response.doneReason(), "unknown"),
                content.length(),
                thinking.length(),
                promptTokens,
                completionTokens,
                latencyMs,
                nanosToMillis(response.totalDuration())
        );

        if (!thinking.isBlank()) {
            log.warn(
                    "Ollama returned a thinking trace although "
                            + "think=false was requested: "
                            + "useCase={}, model={}, thinkingLength={}",
                    useCase,
                    actualModel,
                    thinking.length()
            );
        }

        if (content.isBlank()) {
            throw new OllamaLlmException(
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Ollama response has no final content: useCase="
                            + useCase
                            + ", model="
                            + actualModel
                            + ", doneReason="
                            + valueOrDefault(
                            response.doneReason(),
                            "unknown"
                    )
                            + ", thinkingLength="
                            + thinking.length()
            );
        }

        return new LlmGenerationResponse(
                PROVIDER,
                actualModel,
                content,
                new LlmUsage(
                        promptTokens,
                        completionTokens,
                        promptTokens + completionTokens
                )
        );
    }

    private List<OllamaChatMessage> mapMessages(
            List<LlmMessage> messages
    ) {
        return messages.stream()
                .map(message -> new OllamaChatMessage(
                        message.role()
                                .name()
                                .toLowerCase(Locale.ROOT),
                        message.content()
                ))
                .toList();
    }

    private JsonNode mapResponseFormat(
            LlmGenerationRequest request
    ) {
        if (request.options().responseFormat()
                == LlmResponseFormat.TEXT) {
            return null;
        }

        if (QUESTIONNAIRE_FORM_USE_CASE.equals(
                request.useCase()
        )) {
            return questionnaireFormSchema(
                    requireExpectedQuestionnaireAnswerCount(request)
            );
        }

        if (QUESTIONNAIRE_CHOICE_USE_CASE.equals(
                request.useCase()
        )) {
            return questionnaireChoiceSchema();
        }

        return JsonNodeFactory.instance.textNode("json");
    }

    private int requireExpectedQuestionnaireAnswerCount(
            LlmGenerationRequest request
    ) {
        int expectedAnswerCount =
                request.options().expectedJsonArrayItems();

        if (expectedAnswerCount < 1) {
            throw new IllegalArgumentException(
                    "Questionnaire form LLM request must declare "
                            + "expected JSON answer count"
            );
        }

        return expectedAnswerCount;
    }

    private JsonNode questionnaireFormSchema(
            int expectedAnswerCount
    ) {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();

        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode rootProperties = schema.putObject(
                "properties"
        );

        ObjectNode answers = rootProperties.putObject(
                "answers"
        );

        answers.put("type", "array");
        answers.put("minItems", expectedAnswerCount);
        answers.put("maxItems", expectedAnswerCount);

        ObjectNode answerItem = answers.putObject(
                "items"
        );

        answerItem.put("type", "object");
        answerItem.put("additionalProperties", false);

        ObjectNode answerProperties = answerItem.putObject(
                "properties"
        );

        answerProperties.putObject("questionIndex")
                .put("type", "integer")
                .put("minimum", 1)
                .put("maximum", expectedAnswerCount);

        answerProperties.putObject("status")
                .put("type", "string")
                .putArray("enum")
                .add("ANSWER")
                .add("CANDIDATE_FACT_REQUIRED");

        answerProperties.putObject("answer")
                .put("type", "string")
                .put("maxLength", 300);

        answerProperties.putObject("selectedOptionIndex")
                .put("type", "integer")
                .put("minimum", 0);

        answerProperties.putObject("missingFact")
                .put("type", "string")
                .put("maxLength", 220);

        answerItem.putArray("required")
                .add("questionIndex")
                .add("status")
                .add("answer")
                .add("selectedOptionIndex")
                .add("missingFact");

        schema.putArray("required")
                .add("answers");

        return schema;
    }

    private JsonNode questionnaireChoiceSchema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();

        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject(
                "properties"
        );

        ObjectNode selectedOptionValue = properties.putObject(
                "selectedOptionValue"
        );

        selectedOptionValue.put("type", "string");
        selectedOptionValue.put("minLength", 1);
        selectedOptionValue.put("maxLength", 128);

        ObjectNode selectionReason = properties.putObject(
                "selectionReason"
        );

        selectionReason.put("type", "string");
        selectionReason.put("minLength", 1);
        selectionReason.put("maxLength", 240);

        schema.putArray("required")
                .add("selectedOptionValue")
                .add("selectionReason");

        return schema;
    }

    private int safeInt(
            Integer value
    ) {
        return value == null
                ? 0
                : Math.max(value, 0);
    }

    private long nanosToMillis(
            Long value
    ) {
        return value == null
                ? 0L
                : Math.max(
                TimeUnit.NANOSECONDS.toMillis(value),
                0L
        );
    }

    private String trimToEmpty(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        String normalized = trimToEmpty(value);

        return normalized.isBlank()
                ? defaultValue
                : normalized;
    }
}