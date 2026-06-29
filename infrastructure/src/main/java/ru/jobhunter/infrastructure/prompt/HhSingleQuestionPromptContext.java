package ru.jobhunter.infrastructure.prompt;

import java.util.Map;
import java.util.Objects;

public record HhSingleQuestionPromptContext(
        String candidateProfileFacts,
        String additionalConfirmedFacts,
        String resumeText,
        String fieldName,
        String questionText
) implements PromptTemplateModel {

    public HhSingleQuestionPromptContext {
        candidateProfileFacts = requireText(
                candidateProfileFacts,
                "Candidate profile facts must not be blank"
        );
        additionalConfirmedFacts = requireText(
                additionalConfirmedFacts,
                "Additional confirmed facts must not be blank"
        );
        resumeText = requireText(
                resumeText,
                "Resume text must not be blank"
        );
        fieldName = requireText(
                fieldName,
                "Questionnaire field name must not be blank"
        );
        questionText = requireText(
                questionText,
                "Questionnaire question text must not be blank"
        );
    }

    @Override
    public Map<String, Object> toTemplateModel() {
        return Map.of(
                "candidate_profile_facts", candidateProfileFacts,
                "additional_confirmed_facts", additionalConfirmedFacts,
                "resume_text", resumeText,
                "field_name", fieldName,
                "question_text", questionText
        );
    }

    private static String requireText(
            String value,
            String message
    ) {
        Objects.requireNonNull(value, message);

        String normalized = value.strip();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }
}