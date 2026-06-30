package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.*;
import ru.jobhunter.core.application.usecase.autoresponse.*;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AutoResponseQueueService implements
        AddVacancyToAutoResponseQueueUseCase,
        AddVacanciesToAutoResponseQueueUseCase,
        GetAutoResponseQueueUseCase,
        GetReadyAutoResponseQueueItemsUseCase,
        RemoveAutoResponseQueueItemUseCase,
        UpdateAutoResponseQueueItemStatusUseCase,
        MarkAutoResponseQueueItemsReadyUseCase,
        ReturnAutoResponseQueueItemsToQueuedUseCase
{

    private static final Logger log = LoggerFactory.getLogger(AutoResponseQueueService.class);

    private final AutoResponseQueueRepository queueRepository;

    public AutoResponseQueueService(AutoResponseQueueRepository queueRepository) {
        this.queueRepository = Objects.requireNonNull(
                queueRepository,
                "Auto response queue repository must not be null"
        );
    }

    @Override
    public CompletableFuture<AddVacancyToAutoResponseQueueResultDto> addToQueue(
            AddVacancyToAutoResponseQueueCommand command
    ) {
        Objects.requireNonNull(command, "Add vacancy to auto response queue command must not be null");

        return queueRepository.findByUserIdAndSourceAndExternalVacancyId(
                        command.userId(),
                        command.source(),
                        command.externalVacancyId()
                )
                .thenCompose(existingItem -> {
                    if (existingItem.isPresent()) {
                        log.info(
                                "Vacancy already exists in auto response queue: userId={}, source={}, externalVacancyId={}",
                                command.userId(),
                                command.source(),
                                command.externalVacancyId()
                        );

                        return CompletableFuture.completedFuture(
                                new AddVacancyToAutoResponseQueueResultDto(
                                        toDto(existingItem.get()),
                                        false
                                )
                        );
                    }

                    AutoResponseQueueItem item = AutoResponseQueueItem.create(
                            command.userId(),
                            command.source(),
                            command.externalVacancyId(),
                            command.vacancyName(),
                            command.employerName(),
                            command.areaName(),
                            command.vacancyUrl()
                    );

                    return queueRepository.save(item)
                            .thenApply(savedItem -> {
                                log.info(
                                        "Vacancy added to auto response queue: userId={}, source={}, externalVacancyId={}",
                                        command.userId(),
                                        command.source(),
                                        command.externalVacancyId()
                                );

                                return new AddVacancyToAutoResponseQueueResultDto(
                                        toDto(savedItem),
                                        true
                                );
                            });
                });
    }

    private AutoResponseQueueItemDto toDto(AutoResponseQueueItem item) {
        return new AutoResponseQueueItemDto(
                item.id().value(),
                item.source(),
                item.externalVacancyId(),
                item.vacancyName(),
                item.employerName(),
                item.areaName(),
                item.vacancyUrl(),
                item.status(),
                item.candidateApprovalReason(),
                item.diagnosticDirectory(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    @Override
    public CompletableFuture<List<AutoResponseQueueItemDto>> getQueue(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return queueRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .thenApply(items -> items.stream()
                        .map(this::toDto)
                        .toList());
    }

    @Override
    public CompletableFuture<Boolean> removeFromQueue(RemoveAutoResponseQueueItemCommand command) {
        Objects.requireNonNull(command, "Remove auto response queue item command must not be null");

        return queueRepository.deleteByIdAndUserId(
                        command.itemId(),
                        command.userId()
                )
                .thenApply(deleted -> {
                    if (deleted) {
                        log.info(
                                "Auto response queue item removed: userId={}, itemId={}",
                                command.userId(),
                                command.itemId()
                        );
                    } else {
                        log.info(
                                "Auto response queue item was not found for removal: userId={}, itemId={}",
                                command.userId(),
                                command.itemId()
                        );
                    }

                    return deleted;
                });
    }

    @Override
    public CompletableFuture<Optional<AutoResponseQueueItemDto>> updateStatus(
            UpdateAutoResponseQueueItemStatusCommand command
    ) {
        Objects.requireNonNull(command, "Update auto response queue item status command must not be null");

        return queueRepository.updateStatus(
                        command.itemId(),
                        command.userId(),
                        command.status()
                )
                .thenApply(updatedItem -> {
                    if (updatedItem.isPresent()) {
                        log.info(
                                "Auto response queue item status updated: userId={}, itemId={}, status={}",
                                command.userId(),
                                command.itemId(),
                                command.status()
                        );
                    } else {
                        log.info(
                                "Auto response queue item was not found for status update: userId={}, itemId={}, status={}",
                                command.userId(),
                                command.itemId(),
                                command.status()
                        );
                    }

                    return updatedItem.map(this::toDto);
                });
    }

    @Override
    public CompletableFuture<List<AutoResponseQueueItemDto>> getReadyItems(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return queueRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        AutoResponseQueueStatus.READY
                )
                .thenApply(items -> items.stream()
                        .map(this::toDto)
                        .toList())
                .whenComplete((items, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Ready auto response queue items loaded: userId={}, count={}",
                                userId,
                                items.size()
                        );
                    } else {
                        log.warn(
                                "Failed to load ready auto response queue items: userId={}",
                                userId,
                                throwable
                        );
                    }
                });
    }

    @Override
    public CompletableFuture<AddVacanciesToAutoResponseQueueResultDto> addAllToQueue(
            AddVacanciesToAutoResponseQueueCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Add vacancies to auto response queue command must not be null"
        );

        CompletableFuture<BatchAdditionAccumulator> pipeline =
                CompletableFuture.completedFuture(
                        BatchAdditionAccumulator.empty()
                );

        for (AddVacancyToAutoResponseQueueCommand vacancyCommand
                : command.vacancies()) {

            pipeline = pipeline.thenCompose(accumulator ->
                    addToQueue(vacancyCommand)
                            .handle((result, throwable) -> {
                                if (throwable != null) {
                                    String message = rootMessage(throwable);

                                    log.warn(
                                            "Could not add vacancy to auto response queue "
                                                    + "in batch: userId={}, source={}, "
                                                    + "externalVacancyId={}, reason={}",
                                            vacancyCommand.userId(),
                                            vacancyCommand.source(),
                                            vacancyCommand.externalVacancyId(),
                                            message,
                                            throwable
                                    );

                                    return accumulator.withFailure(
                                            new AddVacancyToAutoResponseQueueFailureDto(
                                                    vacancyCommand.externalVacancyId(),
                                                    vacancyCommand.vacancyName(),
                                                    message
                                            )
                                    );
                                }

                                if (result.created()) {
                                    return accumulator.withAdded();
                                }

                                return accumulator.withAlreadyExists();
                            })
            );
        }

        return pipeline.thenApply(accumulator -> {
            AddVacanciesToAutoResponseQueueResultDto result =
                    new AddVacanciesToAutoResponseQueueResultDto(
                            command.vacancies().size(),
                            accumulator.addedCount(),
                            accumulator.alreadyExistsCount(),
                            accumulator.failures()
                    );

            log.info(
                    "Auto response queue batch addition completed: "
                            + "userId={}, requestedCount={}, addedCount={}, "
                            + "alreadyExistsCount={}, failedCount={}",
                    command.userId(),
                    result.requestedCount(),
                    result.addedCount(),
                    result.alreadyExistsCount(),
                    result.failedCount()
            );

            return result;
        });
    }

    private String rootMessage(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    @Override
    public CompletableFuture<MarkAutoResponseQueueItemsReadyResultDto> markReady(
            MarkAutoResponseQueueItemsReadyCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Mark queue items ready command must not be null"
        );

        CompletableFuture<ReadyBatchAccumulator> pipeline =
                CompletableFuture.completedFuture(
                        ReadyBatchAccumulator.empty()
                );

        for (AutoResponseQueueItemId itemId : command.itemIds()) {
            pipeline = pipeline.thenCompose(accumulator ->
                    markSingleItemReady(
                            command.userId(),
                            itemId
                    ).handle((result, throwable) -> {
                        if (throwable != null) {
                            return accumulator.withFailure(
                                    "itemId="
                                            + itemId.value()
                                            + ": "
                                            + rootMessage(throwable)
                            );
                        }

                        return switch (result) {
                            case MARKED_READY ->
                                    accumulator.withMarkedReady();
                            case ALREADY_READY ->
                                    accumulator.withAlreadyReady();
                            case NOT_ELIGIBLE ->
                                    accumulator.withNotEligible();
                            case NOT_FOUND ->
                                    accumulator.withNotFound();
                        };
                    })
            );
        }

        return pipeline.thenApply(accumulator -> {
            MarkAutoResponseQueueItemsReadyResultDto result =
                    new MarkAutoResponseQueueItemsReadyResultDto(
                            command.itemIds().size(),
                            accumulator.markedReadyCount(),
                            accumulator.alreadyReadyCount(),
                            accumulator.notEligibleCount(),
                            accumulator.notFoundCount(),
                            accumulator.failures()
                    );

            log.info(
                    "Queue items batch-ready operation completed: "
                            + "userId={}, requested={}, markedReady={}, "
                            + "alreadyReady={}, notEligible={}, notFound={}, "
                            + "failed={}",
                    command.userId(),
                    result.requestedCount(),
                    result.markedReadyCount(),
                    result.alreadyReadyCount(),
                    result.notEligibleCount(),
                    result.notFoundCount(),
                    result.failedCount()
            );

            return result;
        });
    }

    private CompletableFuture<ReadyBatchItemResult> markSingleItemReady(
            UserId userId,
            AutoResponseQueueItemId itemId
    ) {
        return queueRepository.findByIdAndUserId(
                        itemId,
                        userId
                )
                .thenCompose(optionalItem -> {
                    if (optionalItem.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                ReadyBatchItemResult.NOT_FOUND
                        );
                    }

                    AutoResponseQueueItem item = optionalItem.get();

                    if (item.status() == AutoResponseQueueStatus.READY) {
                        return CompletableFuture.completedFuture(
                                ReadyBatchItemResult.ALREADY_READY
                        );
                    }

                    if (item.status() != AutoResponseQueueStatus.QUEUED) {
                        return CompletableFuture.completedFuture(
                                ReadyBatchItemResult.NOT_ELIGIBLE
                        );
                    }

                    return queueRepository.updateStatus(
                                    itemId,
                                    userId,
                                    AutoResponseQueueStatus.READY
                            )
                            .thenApply(updatedItem ->
                                    updatedItem.isPresent()
                                            ? ReadyBatchItemResult.MARKED_READY
                                            : ReadyBatchItemResult.NOT_FOUND
                            );
                });
    }



    @Override
    public CompletableFuture<ReturnAutoResponseQueueItemsToQueuedResultDto>
    returnToQueued(
            ReturnAutoResponseQueueItemsToQueuedCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Return queue items to queued command must not be null"
        );

        CompletableFuture<ReturnToQueuedBatchAccumulator> pipeline =
                CompletableFuture.completedFuture(
                        ReturnToQueuedBatchAccumulator.empty()
                );

        for (AutoResponseQueueItemId itemId : command.itemIds()) {
            pipeline = pipeline.thenCompose(accumulator ->
                    returnSingleItemToQueued(
                            command.userId(),
                            itemId
                    ).handle((itemResult, throwable) -> {
                        if (throwable != null) {
                            return accumulator.withFailure(
                                    "itemId="
                                            + itemId.value()
                                            + ": "
                                            + rootMessage(throwable)
                            );
                        }

                        return switch (itemResult) {
                            case RETURNED_TO_QUEUED ->
                                    accumulator.withReturnedToQueued();
                            case ALREADY_QUEUED ->
                                    accumulator.withAlreadyQueued();
                            case NOT_ELIGIBLE ->
                                    accumulator.withNotEligible();
                            case NOT_FOUND ->
                                    accumulator.withNotFound();
                        };
                    })
            );
        }

        return pipeline.thenApply(accumulator -> {
            ReturnAutoResponseQueueItemsToQueuedResultDto result =
                    new ReturnAutoResponseQueueItemsToQueuedResultDto(
                            command.itemIds().size(),
                            accumulator.returnedToQueuedCount(),
                            accumulator.alreadyQueuedCount(),
                            accumulator.notEligibleCount(),
                            accumulator.notFoundCount(),
                            accumulator.failures()
                    );

            log.info(
                    "Queue items batch-return-to-queued operation completed: "
                            + "userId={}, requested={}, returnedToQueued={}, "
                            + "alreadyQueued={}, notEligible={}, notFound={}, "
                            + "failed={}",
                    command.userId(),
                    result.requestedCount(),
                    result.returnedToQueuedCount(),
                    result.alreadyQueuedCount(),
                    result.notEligibleCount(),
                    result.notFoundCount(),
                    result.failedCount()
            );

            return result;
        });
    }

    private CompletableFuture<ReturnToQueuedBatchItemResult>
    returnSingleItemToQueued(
            UserId userId,
            AutoResponseQueueItemId itemId
    ) {
        return queueRepository.findByIdAndUserId(
                        itemId,
                        userId
                )
                .thenCompose(optionalItem -> {
                    if (optionalItem.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                ReturnToQueuedBatchItemResult.NOT_FOUND
                        );
                    }

                    AutoResponseQueueItem item = optionalItem.get();

                    if (item.status() == AutoResponseQueueStatus.QUEUED) {
                        return CompletableFuture.completedFuture(
                                ReturnToQueuedBatchItemResult.ALREADY_QUEUED
                        );
                    }

                    if (item.status() != AutoResponseQueueStatus.READY) {
                        return CompletableFuture.completedFuture(
                                ReturnToQueuedBatchItemResult.NOT_ELIGIBLE
                        );
                    }

                    return queueRepository.updateStatus(
                                    itemId,
                                    userId,
                                    AutoResponseQueueStatus.QUEUED
                            )
                            .thenApply(updatedItem ->
                                    updatedItem.isPresent()
                                            ? ReturnToQueuedBatchItemResult
                                            .RETURNED_TO_QUEUED
                                            : ReturnToQueuedBatchItemResult
                                            .NOT_FOUND
                            );
                });
    }

    private record BatchAdditionAccumulator(
            int addedCount,
            int alreadyExistsCount,
            List<AddVacancyToAutoResponseQueueFailureDto> failures
    ) {

        private static BatchAdditionAccumulator empty() {
            return new BatchAdditionAccumulator(
                    0,
                    0,
                    List.of()
            );
        }

        private BatchAdditionAccumulator withAdded() {
            return new BatchAdditionAccumulator(
                    addedCount + 1,
                    alreadyExistsCount,
                    failures
            );
        }

        private BatchAdditionAccumulator withAlreadyExists() {
            return new BatchAdditionAccumulator(
                    addedCount,
                    alreadyExistsCount + 1,
                    failures
            );
        }

        private BatchAdditionAccumulator withFailure(
                AddVacancyToAutoResponseQueueFailureDto failure
        ) {
            List<AddVacancyToAutoResponseQueueFailureDto> updatedFailures =
                    new java.util.ArrayList<>(failures);

            updatedFailures.add(failure);

            return new BatchAdditionAccumulator(
                    addedCount,
                    alreadyExistsCount,
                    List.copyOf(updatedFailures)
            );
        }
    }

    private enum ReadyBatchItemResult {
        MARKED_READY,
        ALREADY_READY,
        NOT_ELIGIBLE,
        NOT_FOUND
    }

    private record ReadyBatchAccumulator(
            int markedReadyCount,
            int alreadyReadyCount,
            int notEligibleCount,
            int notFoundCount,
            List<String> failures
    ) {

        private static ReadyBatchAccumulator empty() {
            return new ReadyBatchAccumulator(
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        private ReadyBatchAccumulator withMarkedReady() {
            return new ReadyBatchAccumulator(
                    markedReadyCount + 1,
                    alreadyReadyCount,
                    notEligibleCount,
                    notFoundCount,
                    failures
            );
        }

        private ReadyBatchAccumulator withAlreadyReady() {
            return new ReadyBatchAccumulator(
                    markedReadyCount,
                    alreadyReadyCount + 1,
                    notEligibleCount,
                    notFoundCount,
                    failures
            );
        }

        private ReadyBatchAccumulator withNotEligible() {
            return new ReadyBatchAccumulator(
                    markedReadyCount,
                    alreadyReadyCount,
                    notEligibleCount + 1,
                    notFoundCount,
                    failures
            );
        }

        private ReadyBatchAccumulator withNotFound() {
            return new ReadyBatchAccumulator(
                    markedReadyCount,
                    alreadyReadyCount,
                    notEligibleCount,
                    notFoundCount + 1,
                    failures
            );
        }

        private ReadyBatchAccumulator withFailure(
                String failure
        ) {
            List<String> updatedFailures = new ArrayList<>(failures);
            updatedFailures.add(failure);

            return new ReadyBatchAccumulator(
                    markedReadyCount,
                    alreadyReadyCount,
                    notEligibleCount,
                    notFoundCount,
                    List.copyOf(updatedFailures)
            );
        }
    }

    private record ReturnToQueuedBatchAccumulator(
            int returnedToQueuedCount,
            int alreadyQueuedCount,
            int notEligibleCount,
            int notFoundCount,
            List<String> failures
    ) {

        private static ReturnToQueuedBatchAccumulator empty() {
            return new ReturnToQueuedBatchAccumulator(
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        private ReturnToQueuedBatchAccumulator withReturnedToQueued() {
            return new ReturnToQueuedBatchAccumulator(
                    returnedToQueuedCount + 1,
                    alreadyQueuedCount,
                    notEligibleCount,
                    notFoundCount,
                    failures
            );
        }

        private ReturnToQueuedBatchAccumulator withAlreadyQueued() {
            return new ReturnToQueuedBatchAccumulator(
                    returnedToQueuedCount,
                    alreadyQueuedCount + 1,
                    notEligibleCount,
                    notFoundCount,
                    failures
            );
        }

        private ReturnToQueuedBatchAccumulator withNotEligible() {
            return new ReturnToQueuedBatchAccumulator(
                    returnedToQueuedCount,
                    alreadyQueuedCount,
                    notEligibleCount + 1,
                    notFoundCount,
                    failures
            );
        }

        private ReturnToQueuedBatchAccumulator withNotFound() {
            return new ReturnToQueuedBatchAccumulator(
                    returnedToQueuedCount,
                    alreadyQueuedCount,
                    notEligibleCount,
                    notFoundCount + 1,
                    failures
            );
        }

        private ReturnToQueuedBatchAccumulator withFailure(
                String failure
        ) {
            List<String> updatedFailures = new ArrayList<>(failures);
            updatedFailures.add(failure);

            return new ReturnToQueuedBatchAccumulator(
                    returnedToQueuedCount,
                    alreadyQueuedCount,
                    notEligibleCount,
                    notFoundCount,
                    List.copyOf(updatedFailures)
            );
        }
    }

    private enum ReturnToQueuedBatchItemResult {
        RETURNED_TO_QUEUED,
        ALREADY_QUEUED,
        NOT_ELIGIBLE,
        NOT_FOUND
    }
}
