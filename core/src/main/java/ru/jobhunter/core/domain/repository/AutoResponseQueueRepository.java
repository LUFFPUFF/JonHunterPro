package ru.jobhunter.core.domain.repository;

import ru.jobhunter.core.domain.model.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AutoResponseQueueRepository {

    CompletableFuture<AutoResponseQueueItem> save(AutoResponseQueueItem item);

    CompletableFuture<Optional<AutoResponseQueueItem>> findByUserIdAndSourceAndExternalVacancyId(
            UserId userId,
            VacancySource source,
            String externalVacancyId
    );

    CompletableFuture<List<AutoResponseQueueItem>> findByUserIdOrderByCreatedAtDesc(UserId userId);

    CompletableFuture<List<AutoResponseQueueItem>> findByUserIdAndStatusOrderByCreatedAtDesc(
            UserId userId,
            AutoResponseQueueStatus status
    );

    CompletableFuture<Optional<AutoResponseQueueItem>> findByIdAndUserId(
            AutoResponseQueueItemId itemId,
            UserId userId
    );

    CompletableFuture<Boolean> deleteByIdAndUserId(
            AutoResponseQueueItemId itemId,
            UserId userId
    );

    CompletableFuture<Optional<AutoResponseQueueItem>> updateStatus(
            AutoResponseQueueItemId itemId,
            UserId userId,
            AutoResponseQueueStatus status
    );

    CompletableFuture<Optional<AutoResponseQueueItem>> markCandidateApprovalRequired(
            AutoResponseQueueItemId itemId,
            UserId userId,
            String approvalReason,
            String diagnosticDirectory
    );

    CompletableFuture<Optional<AutoResponseQueueItem>> claimReadyForExecution(
            AutoResponseQueueItemId itemId,
            UserId userId
    );
}
