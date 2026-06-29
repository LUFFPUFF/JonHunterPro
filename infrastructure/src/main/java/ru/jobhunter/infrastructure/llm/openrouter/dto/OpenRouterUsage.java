package ru.jobhunter.infrastructure.llm.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterUsage(
        @JsonProperty("prompt_tokens")
        Integer promptTokens,

        @JsonProperty("completion_tokens")
        Integer completionTokens,

        @JsonProperty("total_tokens")
        Integer totalTokens
) {
}