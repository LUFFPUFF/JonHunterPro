package ru.jobhunter.core.application.dto;

import java.util.Locale;

public enum HhQuestionnaireAnswerQuality {

    CONFIRMED(true, false),
    PROFILE_DERIVED(true, false),
    SAFE_DEFAULT(true, false),
    REVIEW_REQUIRED(false, true);

    private final boolean autoFillAllowed;
    private final boolean candidateApprovalRequired;

    HhQuestionnaireAnswerQuality(
            boolean autoFillAllowed,
            boolean candidateApprovalRequired
    ) {
        this.autoFillAllowed = autoFillAllowed;
        this.candidateApprovalRequired = candidateApprovalRequired;
    }

    public boolean isAutoFillAllowed() {
        return autoFillAllowed;
    }

    public boolean requiresCandidateApproval() {
        return candidateApprovalRequired;
    }

    public static HhQuestionnaireAnswerQuality from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Questionnaire answer quality must not be blank"
            );
        }

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        for (HhQuestionnaireAnswerQuality quality : values()) {
            if (quality.name().equals(normalized)) {
                return quality;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported questionnaire answer quality: " + value
        );
    }
}