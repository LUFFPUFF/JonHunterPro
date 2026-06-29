package ru.jobhunter.core.application.dto;

public record HhQuestionnaireOptionDto(
        String value,
        String label
) {

    private static final int MAX_VALUE_LENGTH = 128;
    private static final int MAX_LABEL_LENGTH = 500;

    public HhQuestionnaireOptionDto {
        value = requireNotBlank(
                value,
                "Questionnaire option value must not be blank",
                MAX_VALUE_LENGTH
        );

        label = requireNotBlank(
                label,
                "Questionnaire option label must not be blank",
                MAX_LABEL_LENGTH
        );
    }

    private static String requireNotBlank(
            String value,
            String message,
            int maxLength
    ) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    message + ": maximum length is " + maxLength
            );
        }

        return normalized;
    }
}