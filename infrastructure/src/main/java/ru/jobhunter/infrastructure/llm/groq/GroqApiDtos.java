package ru.jobhunter.infrastructure.llm.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record GroqChatRequest(
        String model,
        List<GroqChatMessage> messages,
        Double temperature,

        @JsonProperty("max_completion_tokens")
        Integer maxCompletionTokens,

        @JsonProperty("response_format")
        GroqResponseFormat responseFormat,

        @JsonProperty("reasoning_effort")
        String reasoningEffort
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record GroqChatMessage(
        String role,
        String content
) {
}

record GroqResponseFormat(String type) {

    static GroqResponseFormat jsonObject() {
        return new GroqResponseFormat("json_object");
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record GroqChatResponse(
        String id,
        String model,
        List<GroqChoice> choices,
        GroqUsage usage
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record GroqChoice(
        Integer index,
        GroqChatMessage message,

        @JsonProperty("finish_reason")
        String finishReason
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record GroqUsage(
        @JsonProperty("prompt_tokens")
        Integer promptTokens,

        @JsonProperty("completion_tokens")
        Integer completionTokens,

        @JsonProperty("total_tokens")
        Integer totalTokens
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record GroqErrorResponse(
        GroqError error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroqError(
            String message,
            String type,
            String code
    ) {
    }
}