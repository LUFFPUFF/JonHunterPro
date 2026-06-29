package ru.jobhunter.infrastructure.prompt;

import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record HhChoiceQuestionPromptContext(
        String candidateProfileFacts,
        String additionalConfirmedFacts,
        String resumeText,
        String fieldName,
        String questionText,
        List<HhQuestionnaireOptionDto> options
) implements PromptTemplateModel {

    public HhChoiceQuestionPromptContext {
        candidateProfileFacts = requireNotBlank(
                candidateProfileFacts,
                "Candidate profile facts must not be blank"
        );

        additionalConfirmedFacts = requireNotBlank(
                additionalConfirmedFacts,
                "Additional confirmed facts must not be blank"
        );

        resumeText = requireNotBlank(
                resumeText,
                "Resume text must not be blank"
        );

        fieldName = requireNotBlank(
                fieldName,
                "Questionnaire field name must not be blank"
        );

        questionText = requireNotBlank(
                questionText,
                "Questionnaire question text must not be blank"
        );

        Objects.requireNonNull(
                options,
                "Questionnaire options must not be null"
        );

        if (options.size() < 2) {
            throw new IllegalArgumentException(
                    "Choice question must contain at least two options"
            );
        }

        options = List.copyOf(options);
    }

    @Override
    public Map<String, Object> toTemplateModel() {
        List<Map<String, String>> renderedOptions = options.stream()
                .map(option -> Map.of(
                        "value", option.value(),
                        "label", option.label()
                ))
                .toList();

        return Map.of(
                "candidate_profile_facts", candidateProfileFacts,
                "additional_confirmed_facts", additionalConfirmedFacts,
                "resume_text", resumeText,
                "field_name", fieldName,
                "question_text", questionText,
                "options", renderedOptions
        );
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }
}