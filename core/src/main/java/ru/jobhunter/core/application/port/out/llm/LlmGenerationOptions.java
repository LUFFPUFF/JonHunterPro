package ru.jobhunter.core.application.port.out.llm;

import java.util.Objects;

public record LlmGenerationOptions(
        double temperature,
        int maxTokens,
        LlmResponseFormat responseFormat,
        int expectedJsonArrayItems
) {

    private static final double MIN_TEMPERATURE = 0.0;
    private static final double MAX_TEMPERATURE = 2.0;
    private static final int MIN_MAX_TOKENS = 1;

    public LlmGenerationOptions(
            double temperature,
            int maxTokens
    ) {
        this(
                temperature,
                maxTokens,
                LlmResponseFormat.TEXT,
                0
        );
    }

    public LlmGenerationOptions(
            double temperature,
            int maxTokens,
            LlmResponseFormat responseFormat
    ) {
        this(
                temperature,
                maxTokens,
                responseFormat,
                0
        );
    }

    public LlmGenerationOptions {
        if (temperature < MIN_TEMPERATURE
                || temperature > MAX_TEMPERATURE) {
            throw new IllegalArgumentException(
                    "LLM temperature must be between 0.0 and 2.0"
            );
        }

        if (maxTokens < MIN_MAX_TOKENS) {
            throw new IllegalArgumentException(
                    "LLM max tokens must be greater than 0"
            );
        }

        Objects.requireNonNull(
                responseFormat,
                "LLM response format must not be null"
        );

        if (expectedJsonArrayItems < 0) {
            throw new IllegalArgumentException(
                    "Expected JSON array items must not be negative"
            );
        }

        if (expectedJsonArrayItems > 0
                && responseFormat != LlmResponseFormat.JSON_OBJECT) {
            throw new IllegalArgumentException(
                    "Expected JSON array items require JSON_OBJECT response format"
            );
        }
    }

    public static LlmGenerationOptions balanced() {
        return new LlmGenerationOptions(
                0.7,
                1200,
                LlmResponseFormat.TEXT
        );
    }

    public static LlmGenerationOptions coverLetter() {
        return new LlmGenerationOptions(
                0.3,
                420,
                LlmResponseFormat.TEXT
        );
    }

    public static LlmGenerationOptions compactCoverLetter() {
        return new LlmGenerationOptions(
                0.0,
                300,
                LlmResponseFormat.TEXT
        );
    }

    public static LlmGenerationOptions deterministic() {
        return new LlmGenerationOptions(
                0.2,
                1200,
                LlmResponseFormat.TEXT
        );
    }

    public static LlmGenerationOptions deterministicJson() {
        return new LlmGenerationOptions(
                0.2,
                1200,
                LlmResponseFormat.JSON_OBJECT
        );
    }

    public static LlmGenerationOptions questionnaireJson() {
        return new LlmGenerationOptions(
                0.0,
                1600,
                LlmResponseFormat.JSON_OBJECT
        );
    }

    public static LlmGenerationOptions questionnaireSingleAnswerJson() {
        return new LlmGenerationOptions(
                0.0,
                700,
                LlmResponseFormat.JSON_OBJECT
        );
    }

    public static LlmGenerationOptions questionnaireChoiceJson() {
        return new LlmGenerationOptions(
                0.0,
                400,
                LlmResponseFormat.JSON_OBJECT
        );
    }

    public static LlmGenerationOptions questionnaireFormJson() {
        return new LlmGenerationOptions(
                0.0,
                1200,
                LlmResponseFormat.JSON_OBJECT
        );
    }

    public static LlmGenerationOptions questionnaireFormJson(
            int expectedAnswerCount
    ) {
        if (expectedAnswerCount < 1) {
            throw new IllegalArgumentException(
                    "Questionnaire form must contain at least one question"
            );
        }

        int maxTokens = Math.min(
                1600,
                Math.max(
                        800,
                        expectedAnswerCount * 200
                )
        );

        return new LlmGenerationOptions(
                0.0,
                maxTokens,
                LlmResponseFormat.JSON_OBJECT,
                expectedAnswerCount
        );
    }
}