package ru.jobhunter.core.application.port.out.llm;

import java.util.Objects;

public record LlmGenerationResponse(
        String provider,
        String model,
        String content,
        LlmUsage usage
) {

    public LlmGenerationResponse {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("LLM provider must not be blank");
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("LLM model must not be blank");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("LLM response content must not be blank");
        }

        Objects.requireNonNull(usage, "LLM usage must not be null");

        provider = provider.trim();
        model = model.trim();
        content = content.trim();
    }
}