package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import java.util.Objects;

public record HhBrowserAutoResponseResult(
        HhBrowserAutoResponseOutcome outcome,
        String candidateApprovalReason,
        String diagnosticDirectory
) {

    public HhBrowserAutoResponseResult {
        Objects.requireNonNull(
                outcome,
                "HH browser auto response outcome must not be null"
        );

        candidateApprovalReason = normalize(candidateApprovalReason);
        diagnosticDirectory = normalize(diagnosticDirectory);
    }

    public static HhBrowserAutoResponseResult completed(
            HhBrowserAutoResponseOutcome outcome,
            String diagnosticDirectory
    ) {
        return new HhBrowserAutoResponseResult(
                outcome,
                null,
                diagnosticDirectory
        );
    }

    public static HhBrowserAutoResponseResult candidateApprovalRequired(
            String candidateApprovalReason,
            String diagnosticDirectory
    ) {
        String normalizedReason = normalize(candidateApprovalReason);

        if (normalizedReason == null) {
            throw new IllegalArgumentException(
                    "Candidate approval reason must not be blank"
            );
        }

        return new HhBrowserAutoResponseResult(
                HhBrowserAutoResponseOutcome.QUESTIONNAIRE_REQUIRED,
                normalizedReason,
                diagnosticDirectory
        );
    }

    public boolean requiresCandidateApproval() {
        return candidateApprovalReason != null;
    }

    private static String normalize(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}