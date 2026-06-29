package ru.jobhunter.core.application.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record HhQuestionnaireQuestionDto(
        String fieldName,
        String questionText,
        HhQuestionnaireFieldType fieldType,
        List<HhQuestionnaireOptionDto> options,
        String otherOptionValue,
        String otherTextFieldName
) {

    public HhQuestionnaireQuestionDto(
            String fieldName,
            String questionText
    ) {
        this(
                fieldName,
                questionText,
                HhQuestionnaireFieldType.TEXT,
                List.of(),
                "",
                ""
        );
    }

    public HhQuestionnaireQuestionDto(
            String fieldName,
            String questionText,
            HhQuestionnaireFieldType fieldType,
            List<HhQuestionnaireOptionDto> options
    ) {
        this(
                fieldName,
                questionText,
                fieldType,
                options,
                "",
                ""
        );
    }

    public HhQuestionnaireQuestionDto(
            String fieldName,
            String questionText,
            HhQuestionnaireFieldType fieldType,
            List<HhQuestionnaireOptionDto> options,
            String otherOptionValue,
            String otherTextFieldName
    ) {
        String normalizedFieldName = requireNotBlank(
                fieldName,
                "Questionnaire field name must not be blank"
        );

        String normalizedQuestionText = requireNotBlank(
                questionText,
                "Questionnaire question text must not be blank"
        );

        HhQuestionnaireFieldType normalizedFieldType =
                Objects.requireNonNull(
                        fieldType,
                        "Questionnaire field type must not be null"
                );

        Objects.requireNonNull(
                options,
                "Questionnaire options must not be null"
        );

        List<HhQuestionnaireOptionDto> copiedOptions =
                List.copyOf(options);

        String normalizedOtherOptionValue =
                normalize(otherOptionValue);

        String normalizedOtherTextFieldName =
                normalize(otherTextFieldName);

        switch (normalizedFieldType) {
            case TEXT -> validateTextField(
                    copiedOptions,
                    normalizedOtherOptionValue,
                    normalizedOtherTextFieldName
            );

            case RADIO -> validateRadioField(
                    copiedOptions,
                    normalizedOtherOptionValue,
                    normalizedOtherTextFieldName
            );

            case RADIO_WITH_OTHER_TEXT -> validateRadioWithOtherTextField(
                    normalizedFieldName,
                    copiedOptions,
                    normalizedOtherOptionValue,
                    normalizedOtherTextFieldName
            );
        }

        this.fieldName = normalizedFieldName;
        this.questionText = normalizedQuestionText;
        this.fieldType = normalizedFieldType;
        this.options = copiedOptions;
        this.otherOptionValue = normalizedOtherOptionValue;
        this.otherTextFieldName = normalizedOtherTextFieldName;
    }

    public boolean isText() {
        return fieldType == HhQuestionnaireFieldType.TEXT;
    }

    public boolean isRadio() {
        return fieldType == HhQuestionnaireFieldType.RADIO;
    }

    public boolean isRadioWithOtherText() {
        return fieldType
                == HhQuestionnaireFieldType.RADIO_WITH_OTHER_TEXT;
    }

    private static void validateTextField(
            List<HhQuestionnaireOptionDto> options,
            String otherOptionValue,
            String otherTextFieldName
    ) {
        if (!options.isEmpty()
                || !otherOptionValue.isBlank()
                || !otherTextFieldName.isBlank()) {
            throw new IllegalArgumentException(
                    "Text questionnaire field must not contain options "
                            + "or other-text metadata"
            );
        }
    }

    private static void validateRadioField(
            List<HhQuestionnaireOptionDto> options,
            String otherOptionValue,
            String otherTextFieldName
    ) {
        validateChoiceOptions(options);

        if (!otherOptionValue.isBlank()
                || !otherTextFieldName.isBlank()) {
            throw new IllegalArgumentException(
                    "Radio questionnaire field must not contain "
                            + "other-text metadata"
            );
        }
    }

    private static void validateRadioWithOtherTextField(
            String fieldName,
            List<HhQuestionnaireOptionDto> options,
            String otherOptionValue,
            String otherTextFieldName
    ) {
        validateChoiceOptions(options);

        if (otherOptionValue.isBlank()
                || otherTextFieldName.isBlank()) {
            throw new IllegalArgumentException(
                    "Radio-with-other-text field must contain "
                            + "other option metadata"
            );
        }

        boolean optionExists = options.stream()
                .anyMatch(option ->
                        option.value().equals(otherOptionValue)
                );

        if (!optionExists) {
            throw new IllegalArgumentException(
                    "Other option value must exist in radio options"
            );
        }

        String expectedOtherTextFieldName = fieldName + "_text";

        if (!expectedOtherTextFieldName.equals(
                otherTextFieldName
        )) {
            throw new IllegalArgumentException(
                    "Unexpected other-text field name: expected "
                            + expectedOtherTextFieldName
                            + ", actual "
                            + otherTextFieldName
            );
        }
    }

    private static void validateChoiceOptions(
            List<HhQuestionnaireOptionDto> options
    ) {
        if (options.size() < 2) {
            throw new IllegalArgumentException(
                    "Choice questionnaire field must contain "
                            + "at least two options"
            );
        }

        Set<String> values = new HashSet<>();

        for (HhQuestionnaireOptionDto option : options) {
            Objects.requireNonNull(
                    option,
                    "Questionnaire option must not be null"
            );

            if (!values.add(option.value())) {
                throw new IllegalArgumentException(
                        "Questionnaire options must have unique values"
                );
            }
        }
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private static String normalize(
            String value
    ) {
        return value == null ? "" : value.trim();
    }
}