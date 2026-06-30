package ru.jobhunter.infrastructure.platform.habr.autoresponse;

import java.util.Objects;

public record HabrCareerBrowserAutoResponseResult(
        HabrCareerBrowserAutoResponseOutcome outcome,
        String message,
        String candidateApprovalReason,
        String diagnosticDirectory
) {

    public HabrCareerBrowserAutoResponseResult {
        Objects.requireNonNull(outcome, "Habr Career browser outcome must not be null");
        message = normalize(message);
        candidateApprovalReason = normalize(candidateApprovalReason);
        diagnosticDirectory = normalize(diagnosticDirectory);

        if (outcome == HabrCareerBrowserAutoResponseOutcome.CANDIDATE_APPROVAL_REQUIRED) {
            if (candidateApprovalReason == null || candidateApprovalReason.isBlank()) {
                throw new IllegalArgumentException(
                        "Candidate approval reason must not be blank"
                );
            }
            if (diagnosticDirectory == null || diagnosticDirectory.isBlank()) {
                throw new IllegalArgumentException(
                        "Diagnostic directory must not be blank"
                );
            }
        }
    }

    public boolean requiresCandidateApproval() {
        return outcome == HabrCareerBrowserAutoResponseOutcome
                .CANDIDATE_APPROVAL_REQUIRED;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
