package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public record AutoResponseQueueItem(
        AutoResponseQueueItemId id,
        UserId userId,
        VacancySource source,
        String externalVacancyId,
        String vacancyName,
        String employerName,
        String areaName,
        String vacancyUrl,
        AutoResponseQueueStatus status,
        String candidateApprovalReason,
        String diagnosticDirectory,
        Instant createdAt,
        Instant updatedAt
) {

    public AutoResponseQueueItem {
        Objects.requireNonNull(
                id,
                "Auto response queue item id must not be null"
        );
        Objects.requireNonNull(
                userId,
                "User id must not be null"
        );
        Objects.requireNonNull(
                source,
                "Vacancy source must not be null"
        );
        Objects.requireNonNull(
                status,
                "Auto response queue status must not be null"
        );
        Objects.requireNonNull(
                createdAt,
                "Created timestamp must not be null"
        );
        Objects.requireNonNull(
                updatedAt,
                "Updated timestamp must not be null"
        );

        externalVacancyId = requireNotBlank(
                externalVacancyId,
                "External vacancy id must not be blank"
        );

        vacancyName = requireNotBlank(
                vacancyName,
                "Vacancy name must not be blank"
        );

        employerName = normalize(employerName);
        areaName = normalize(areaName);
        vacancyUrl = normalize(vacancyUrl);
        candidateApprovalReason = normalize(
                candidateApprovalReason
        );
        diagnosticDirectory = normalize(
                diagnosticDirectory
        );

        if (status == AutoResponseQueueStatus
                .WAITING_CANDIDATE_APPROVAL) {

            candidateApprovalReason = requireNotBlank(
                    candidateApprovalReason,
                    "Candidate approval reason must not be blank"
            );

            diagnosticDirectory = requireNotBlank(
                    diagnosticDirectory,
                    "Diagnostic directory must not be blank"
            );
        } else {
            candidateApprovalReason = null;
            diagnosticDirectory = null;
        }
    }

    public AutoResponseQueueItem(
            AutoResponseQueueItemId id,
            UserId userId,
            VacancySource source,
            String externalVacancyId,
            String vacancyName,
            String employerName,
            String areaName,
            String vacancyUrl,
            AutoResponseQueueStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                userId,
                source,
                externalVacancyId,
                vacancyName,
                employerName,
                areaName,
                vacancyUrl,
                status,
                null,
                null,
                createdAt,
                updatedAt
        );
    }

    public static AutoResponseQueueItem create(
            UserId userId,
            VacancySource source,
            String externalVacancyId,
            String vacancyName,
            String employerName,
            String areaName,
            String vacancyUrl
    ) {
        Instant now = Instant.now();

        return new AutoResponseQueueItem(
                AutoResponseQueueItemId.newId(),
                userId,
                source,
                externalVacancyId,
                vacancyName,
                employerName,
                areaName,
                vacancyUrl,
                AutoResponseQueueStatus.QUEUED,
                null,
                null,
                now,
                now
        );
    }

    public AutoResponseQueueItem withStatus(AutoResponseQueueStatus newStatus) {
        Objects.requireNonNull(newStatus, "Auto response queue status must not be null");
        if (status == AutoResponseQueueStatus.PARTIAL_SUCCESS && newStatus != AutoResponseQueueStatus.PARTIAL_SUCCESS) {
            throw new IllegalStateException("Queue item with PARTIAL_SUCCESS is terminal and " + "must not be returned to another status");
        }
        if (newStatus == AutoResponseQueueStatus.WAITING_CANDIDATE_APPROVAL) {
            throw new IllegalArgumentException("Use withCandidateApprovalRequired " + "for WAITING_CANDIDATE_APPROVAL status");
        }
        return new AutoResponseQueueItem(id, userId, source, externalVacancyId, vacancyName, employerName, areaName, vacancyUrl, newStatus, null, null, createdAt, Instant.now());
    }

    public AutoResponseQueueItem withCandidateApprovalRequired(
            String approvalReason,
            String diagnosticsPath
    ) {
        return new AutoResponseQueueItem(
                id,
                userId,
                source,
                externalVacancyId,
                vacancyName,
                employerName,
                areaName,
                vacancyUrl,
                AutoResponseQueueStatus
                        .WAITING_CANDIDATE_APPROVAL,
                approvalReason,
                diagnosticsPath,
                createdAt,
                Instant.now()
        );
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
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