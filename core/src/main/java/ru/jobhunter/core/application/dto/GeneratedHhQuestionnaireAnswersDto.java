package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record GeneratedHhQuestionnaireAnswersDto(List<GeneratedHhQuestionnaireAnswerDto> answers, String provider,
                                                 String model) {
    public GeneratedHhQuestionnaireAnswersDto {
        Objects.requireNonNull(answers, "Questionnaire answers must not be null");
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("Questionnaire answers must not be empty");
        }
        answers = List.copyOf(answers);
        provider = requireNotBlank(provider, "LLM provider must not be blank");
        model = requireNotBlank(model, "LLM model must not be blank");
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
