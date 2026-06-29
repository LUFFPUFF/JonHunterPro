package ru.jobhunter.core.application.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record GeneratedHhQuestionnaireAnswerDto(
        String fieldName,
        String answer,
        String selectedOptionValue,
        HhQuestionnaireAnswerQuality quality,
        String reviewReason,
        List<String> evidence
) {

    private static final int MAX_ANSWER_LENGTH = 1_200;
    private static final int MAX_OPTION_VALUE_LENGTH = 128;
    private static final int MAX_REVIEW_REASON_LENGTH = 500;
    private static final int MAX_EVIDENCE_ITEM_LENGTH = 300;
    private static final int MAX_EVIDENCE_ITEMS = 12;

    private static final List<String> FORBIDDEN_TECHNICAL_PHRASES = List.of(
            "требует уточнения кандидатом",
            "требует уточнения",
            "необходимо уточнить",
            "информация отсутствует",
            "я не знаю"
    );

    public GeneratedHhQuestionnaireAnswerDto(
            String fieldName,
            String answer,
            HhQuestionnaireAnswerQuality quality,
            String reviewReason,
            List<String> evidence
    ) {
        this(
                fieldName,
                answer,
                "",
                quality,
                reviewReason,
                evidence
        );
    }

    public GeneratedHhQuestionnaireAnswerDto {
        fieldName = requireNotBlank(
                fieldName,
                "Questionnaire answer field name must not be blank"
        );

        answer = normalize(answer);
        selectedOptionValue = normalize(selectedOptionValue);
        reviewReason = normalize(reviewReason);

        quality = Objects.requireNonNull(
                quality,
                "Questionnaire answer quality must not be null"
        );

        evidence = normalizeEvidence(evidence);

        if (answer.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException(
                    "Questionnaire answer is too long"
            );
        }

        if (selectedOptionValue.length() > MAX_OPTION_VALUE_LENGTH) {
            throw new IllegalArgumentException(
                    "Questionnaire selected option value is too long"
            );
        }

        if (reviewReason.length() > MAX_REVIEW_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "Questionnaire review reason is too long"
            );
        }

        if (!answer.isBlank()
                && containsForbiddenTechnicalPhrase(answer)) {
            throw new IllegalArgumentException(
                    "Questionnaire answer contains a technical review phrase"
            );
        }

        if (quality.isAutoFillAllowed()) {
            if (answer.isBlank()
                    && selectedOptionValue.isBlank()) {
                throw new IllegalArgumentException(
                        "Automatically filled questionnaire answer must contain "
                                + "text or selected option value"
                );
            }

            if (evidence.isEmpty()) {
                throw new IllegalArgumentException(
                        "Automatically filled questionnaire answer "
                                + "must contain evidence"
                );
            }

            reviewReason = "";
        }

        if (!selectedOptionValue.isBlank()
                && !quality.isAutoFillAllowed()) {
            throw new IllegalArgumentException(
                    "Only automatically filled questionnaire answer may "
                            + "contain selected option value"
            );
        }

        if (quality.requiresCandidateApproval()
                && reviewReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Review-required questionnaire answer must "
                            + "contain review reason"
            );
        }

        if (quality == HhQuestionnaireAnswerQuality.REVIEW_REQUIRED
                && reviewReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Review-required questionnaire answer must "
                            + "contain review reason"
            );
        }
    }

    private static List<String> normalizeEvidence(
            List<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();

        for (String value : values) {
            String item = normalize(value);

            if (item.isBlank()) {
                continue;
            }

            if (item.length() > MAX_EVIDENCE_ITEM_LENGTH) {
                throw new IllegalArgumentException(
                        "Questionnaire evidence item is too long"
                );
            }

            normalized.add(item);

            if (normalized.size() > MAX_EVIDENCE_ITEMS) {
                throw new IllegalArgumentException(
                        "Too many questionnaire evidence items"
                );
            }
        }

        return List.copyOf(normalized);
    }

    private static boolean containsForbiddenTechnicalPhrase(
            String value
    ) {
        String normalized = value.toLowerCase(Locale.ROOT);

        return FORBIDDEN_TECHNICAL_PHRASES.stream()
                .anyMatch(normalized::contains);
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}