package ru.jobhunter.infrastructure.llm.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterChatResponse(
        String id,
        String model,
        List<OpenRouterChoice> choices,
        OpenRouterUsage usage
) {
}