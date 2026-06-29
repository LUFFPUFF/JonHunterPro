package ru.jobhunter.infrastructure.llm.openrouter;

import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;

import java.util.List;
import java.util.stream.Collectors;

public final class OpenRouterModelsExhaustedException
        extends OpenRouterLlmException {

    public OpenRouterModelsExhaustedException(
            List<String> attemptedModels,
            Throwable cause
    ) {
        super(
                LlmFailureCategory.OPENROUTER_MODELS_EXHAUSTED,
                "OpenRouter did not produce a usable response after "
                        + "all configured models were attempted: "
                        + formatModels(attemptedModels),
                cause
        );
    }

    private static String formatModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return "not-configured";
        }

        String formattedModels = models.stream()
                .filter(model -> model != null && !model.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(", "));

        return formattedModels.isBlank()
                ? "not-configured"
                : formattedModels;
    }
}