package ru.jobhunter.infrastructure.llm.openrouter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterErrorResponse(
        OpenRouterError error
) {

    public record OpenRouterError(
            Integer code,
            String message
    ) {
    }
}