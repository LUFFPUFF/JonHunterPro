package ru.jobhunter.infrastructure.llm.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterChoice(
        Integer index,
        OpenRouterChatMessage message,

        @JsonProperty("finish_reason")
        String finishReason
) {
}