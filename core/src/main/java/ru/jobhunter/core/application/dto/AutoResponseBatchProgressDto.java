package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AutoResponseBatchProgressDto(
        UUID batchId,
        UUID userId,
        AutoResponseBatchProgressStatus status,
        int plannedCount,
        int startedCount,
        int sentCount,
        int partialSuccessCount,
        int candidateApprovalRequiredCount,
        int returnedToReadyCount,
        int failedCount,
        int skippedCount,
        String habrStreamPauseReason,
        Instant startedAt,
        Instant finishedAt
) {

    public AutoResponseBatchProgressDto {
        Objects.requireNonNull(batchId, "Batch id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(status, "Batch progress status must not be null");
        Objects.requireNonNull(startedAt, "Batch start time must not be null");

        if (plannedCount < 1
                || startedCount < 0
                || sentCount < 0
                || partialSuccessCount < 0
                || candidateApprovalRequiredCount < 0
                || returnedToReadyCount < 0
                || failedCount < 0
                || skippedCount < 0) {
            throw new IllegalArgumentException(
                    "Batch progress counters must not be negative"
            );
        }

        if (startedCount > plannedCount) {
            throw new IllegalArgumentException(
                    "Started count must not exceed planned count"
            );
        }

        if (processedCount() > startedCount) {
            throw new IllegalArgumentException(
                    "Processed count must not exceed started count"
            );
        }

        boolean completed = status == AutoResponseBatchProgressStatus.COMPLETED
                || status == AutoResponseBatchProgressStatus.COMPLETED_WITH_ISSUES;

        if (completed && finishedAt == null) {
            throw new IllegalArgumentException(
                    "Completed batch must contain finish time"
            );
        }

        if (!completed && finishedAt != null) {
            throw new IllegalArgumentException(
                    "Active batch must not contain finish time"
            );
        }

        habrStreamPauseReason = normalize(habrStreamPauseReason);
    }

    public int processedCount() {
        return sentCount
                + partialSuccessCount
                + candidateApprovalRequiredCount
                + returnedToReadyCount
                + failedCount
                + skippedCount;
    }

    public boolean isActive() {
        return status == AutoResponseBatchProgressStatus.PREPARING
                || status == AutoResponseBatchProgressStatus.RUNNING;
    }

    public boolean isHabrStreamPaused() {
        return habrStreamPauseReason != null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
