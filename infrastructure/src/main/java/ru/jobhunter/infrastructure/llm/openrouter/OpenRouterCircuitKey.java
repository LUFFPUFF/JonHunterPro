package ru.jobhunter.infrastructure.llm.openrouter;

import java.util.Locale;
import java.util.Objects;

public final class OpenRouterCircuitKey {

    private static final String MODEL_PREFIX = "openrouter:model:";

    private OpenRouterCircuitKey() {
    }

    public static String forModel(String model) {
        return MODEL_PREFIX + normalizeModel(model);
    }

    public static boolean isModelCircuitKey(String circuitKey) {
        return circuitKey != null
                && circuitKey.startsWith(MODEL_PREFIX)
                && circuitKey.length() > MODEL_PREFIX.length();
    }

    private static String normalizeModel(String model) {
        Objects.requireNonNull(
                model,
                "OpenRouter model must not be null"
        );

        String normalized = model.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "OpenRouter model must not be blank"
            );
        }

        return normalized.toLowerCase(Locale.ROOT);
    }
}