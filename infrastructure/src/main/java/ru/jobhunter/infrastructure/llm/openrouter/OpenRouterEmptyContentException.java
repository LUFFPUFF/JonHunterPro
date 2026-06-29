package ru.jobhunter.infrastructure.llm.openrouter;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

public final class OpenRouterEmptyContentException
        extends OpenRouterLlmException {

    public OpenRouterEmptyContentException(
            String model,
            String reason
    ) {
        super(
                LlmFailureCategory.OPENROUTER_EMPTY_CONTENT,
                "OpenRouter response has no usable content: model="
                        + normalize(model, "unknown")
                        + ", reason="
                        + normalize(reason, "unknown")
        );
    }

    private static String normalize(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback
                : value.trim();
    }
}