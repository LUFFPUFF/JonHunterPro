package ru.jobhunter.infrastructure.llm.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterChatRequest(String model, List<OpenRouterChatMessage> messages, Double temperature,
                                    @JsonProperty("max_tokens") Integer maxTokens,
                                    @JsonProperty("response_format") OpenRouterResponseFormat responseFormat) {
}