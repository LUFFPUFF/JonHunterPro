package ru.jobhunter.infrastructure.service.batch;

import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AutoResponseBatchProgressDto;
import ru.jobhunter.core.application.dto.AutoResponseBatchProgressStatus;
import ru.jobhunter.core.domain.model.UserId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public final class AutoResponseBatchProgressStore {

    private final Object monitor = new Object();

    private final Map<UUID, MutableBatchProgress> batches =
            new HashMap<>();

    private UUID activeBatchId;

    BatchRegistration tryStart(
            UserId userId,
            int plannedCount
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        if (plannedCount < 1) {
            throw new IllegalArgumentException(
                    "Planned count must be greater than 0"
            );
        }

        synchronized (monitor) {
            if (activeBatchId != null) {
                MutableBatchProgress activeBatch =
                        batches.get(activeBatchId);

                if (activeBatch != null && activeBatch.isActive()) {
                    return BatchRegistration.alreadyRunning(
                            activeBatch.snapshot()
                    );
                }

                activeBatchId = null;
            }

            UUID batchId = UUID.randomUUID();

            MutableBatchProgress progress =
                    new MutableBatchProgress(
                            batchId,
                            userId.value(),
                            plannedCount
                    );

            batches.put(batchId, progress);
            activeBatchId = batchId;

            return BatchRegistration.started(progress.snapshot());
        }
    }

    void markRunning(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).markRunning();
        }
    }

    void markItemStarted(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).markItemStarted();
        }
    }

    void recordPartialSuccess(UUID batchId) {
        synchronized (monitor) {
            findBatch(batchId).recordPartialSuccess();
        }
    }

    void recordSent(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).recordSent();
        }
    }

    void recordCandidateApprovalRequired(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).recordCandidateApprovalRequired();
        }
    }

    void recordReturnedToReady(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).recordReturnedToReady();
        }
    }

    void recordFailed(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).recordFailed();
        }
    }

    void recordSkipped(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).recordSkipped();
        }
    }

    void markWorkerFailure(
            UUID batchId
    ) {
        synchronized (monitor) {
            findBatch(batchId).recordFailed();
        }
    }

    void complete(
            UUID batchId
    ) {
        synchronized (monitor) {
            MutableBatchProgress progress = findBatch(batchId);

            progress.complete();

            if (batchId.equals(activeBatchId)) {
                activeBatchId = null;
            }
        }
    }

    public Optional<AutoResponseBatchProgressDto> get(
            UUID batchId
    ) {
        Objects.requireNonNull(batchId, "Batch id must not be null");

        synchronized (monitor) {
            MutableBatchProgress progress = batches.get(batchId);

            return progress == null
                    ? Optional.empty()
                    : Optional.of(progress.snapshot());
        }
    }

    private MutableBatchProgress findBatch(
            UUID batchId
    ) {
        MutableBatchProgress progress = batches.get(batchId);

        if (progress == null) {
            throw new IllegalArgumentException(
                    "Auto response batch was not found: " + batchId
            );
        }

        return progress;
    }

    record BatchRegistration(
            boolean started,
            AutoResponseBatchProgressDto progress
    ) {

        static BatchRegistration started(
                AutoResponseBatchProgressDto progress
        ) {
            return new BatchRegistration(true, progress);
        }

        static BatchRegistration alreadyRunning(
                AutoResponseBatchProgressDto progress
        ) {
            return new BatchRegistration(false, progress);
        }
    }

    private static final class MutableBatchProgress {

        private final UUID batchId;
        private final UUID userId;
        private final int plannedCount;
        private final Instant startedAt;

        private AutoResponseBatchProgressStatus status =
                AutoResponseBatchProgressStatus.PREPARING;

        private int startedCount;
        private int sentCount;
        private int partialSuccessCount;
        private int candidateApprovalRequiredCount;
        private int returnedToReadyCount;
        private int failedCount;
        private int skippedCount;

        private Instant finishedAt;

        private MutableBatchProgress(
                UUID batchId,
                UUID userId,
                int plannedCount
        ) {
            this.batchId = batchId;
            this.userId = userId;
            this.plannedCount = plannedCount;
            this.startedAt = Instant.now();
        }

        private boolean isActive() {
            return status == AutoResponseBatchProgressStatus.PREPARING
                    || status == AutoResponseBatchProgressStatus.RUNNING;
        }

        private void markRunning() {
            if (status == AutoResponseBatchProgressStatus.PREPARING) {
                status = AutoResponseBatchProgressStatus.RUNNING;
            }
        }

        private void markItemStarted() {
            startedCount++;
        }

        private void recordSent() {
            sentCount++;
        }

        private void recordPartialSuccess() { partialSuccessCount++; }

        private void recordCandidateApprovalRequired() {
            candidateApprovalRequiredCount++;
        }

        private void recordReturnedToReady() {
            returnedToReadyCount++;
        }

        private void recordFailed() {
            failedCount++;
        }

        private void recordSkipped() {
            skippedCount++;
        }

        private void complete() {
            boolean hasIssues = partialSuccessCount > 0 || candidateApprovalRequiredCount > 0 || returnedToReadyCount > 0 || failedCount > 0 || skippedCount > 0;

            status = hasIssues
                    ? AutoResponseBatchProgressStatus.COMPLETED_WITH_ISSUES
                    : AutoResponseBatchProgressStatus.COMPLETED;

            finishedAt = Instant.now();
        }

        private AutoResponseBatchProgressDto snapshot() {
            return new AutoResponseBatchProgressDto(
                    batchId,
                    userId,
                    status,
                    plannedCount,
                    startedCount,
                    sentCount,
                    partialSuccessCount,
                    candidateApprovalRequiredCount,
                    returnedToReadyCount,
                    failedCount,
                    skippedCount,
                    startedAt,
                    finishedAt
            );
        }
    }
}