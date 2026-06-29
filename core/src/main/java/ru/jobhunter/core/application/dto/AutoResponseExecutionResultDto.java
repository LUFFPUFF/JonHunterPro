package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.AutoResponseExecutionStatus;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.time.Instant;
import java.util.Objects;

public record AutoResponseExecutionResultDto(
        AutoResponseQueueItemId queueItemId,
        VacancySource source,
        String externalVacancyId,
        AutoResponseExecutionStatus status,
        String message,
        String candidateApprovalReason,
        String diagnosticDirectory,
        Instant executedAt
) {

    public AutoResponseExecutionResultDto {
        Objects.requireNonNull(queueItemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(source, "Vacancy source must not be null");
        Objects.requireNonNull(status, "Auto response execution status must not be null");
        Objects.requireNonNull(executedAt, "Executed timestamp must not be null");

        externalVacancyId = requireNotBlank(externalVacancyId, "External vacancy id must not be blank");
        message = normalize(message);

        candidateApprovalReason = normalize(
                candidateApprovalReason
        );

        diagnosticDirectory = normalize(
                diagnosticDirectory
        );

        if (status == AutoResponseExecutionStatus
                .CANDIDATE_APPROVAL_REQUIRED) {

            if (candidateApprovalReason == null) {
                throw new IllegalArgumentException(
                        "Candidate approval reason must not be blank"
                );
            }

            if (diagnosticDirectory == null) {
                throw new IllegalArgumentException(
                        "Diagnostic directory must not be blank"
                );
            }
        }
    }

    public static AutoResponseExecutionResultDto success(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.SUCCESS,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto partialSuccess(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.PARTIAL_SUCCESS,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto preflightCompleted(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.PREFLIGHT_COMPLETED,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto questionnaireFilledReviewRequired(AutoResponseQueueItemId queueItemId, VacancySource source, String externalVacancyId, String message) {
        return new AutoResponseExecutionResultDto(queueItemId, source, externalVacancyId, AutoResponseExecutionStatus.QUESTIONNAIRE_FILLED_REVIEW_REQUIRED, message, null, null, Instant.now());
    }

    public static AutoResponseExecutionResultDto alreadyResponded(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.ALREADY_RESPONDED,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto questionnaireRequired(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.QUESTIONNAIRE_REQUIRED,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto failed(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.FAILED,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto notAvailable(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String message
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus.NOT_AVAILABLE,
                message,
                null,
                null,
                Instant.now()
        );
    }

    public static AutoResponseExecutionResultDto
    candidateApprovalRequired(
            AutoResponseQueueItemId queueItemId,
            VacancySource source,
            String externalVacancyId,
            String candidateApprovalReason,
            String diagnosticDirectory
    ) {
        return new AutoResponseExecutionResultDto(
                queueItemId,
                source,
                externalVacancyId,
                AutoResponseExecutionStatus
                        .CANDIDATE_APPROVAL_REQUIRED,
                "Отклик остановлен: требуется подтверждение "
                        + "кандидата.",
                candidateApprovalReason,
                diagnosticDirectory,
                Instant.now()
        );
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
