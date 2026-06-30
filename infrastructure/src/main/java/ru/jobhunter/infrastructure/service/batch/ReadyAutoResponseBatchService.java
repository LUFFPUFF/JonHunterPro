package ru.jobhunter.infrastructure.service.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.AutoResponseBatchProgressDto;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.application.dto.ExecuteAutoResponseCommand;
import ru.jobhunter.core.application.dto.StartReadyAutoResponsesBatchCommand;
import ru.jobhunter.core.application.dto.StartReadyAutoResponsesBatchResultDto;
import ru.jobhunter.core.application.exception.AutoResponseQueueItemNotReadyException;
import ru.jobhunter.core.application.usecase.autoresponse.ExecuteAutoResponseUseCase;
import ru.jobhunter.core.application.usecase.autoresponse.GetAutoResponseBatchProgressUseCase;
import ru.jobhunter.core.application.usecase.autoresponse.GetReadyAutoResponseQueueItemsUseCase;
import ru.jobhunter.core.application.usecase.autoresponse.StartReadyAutoResponsesBatchUseCase;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public final class ReadyAutoResponseBatchService implements
        StartReadyAutoResponsesBatchUseCase,
        GetAutoResponseBatchProgressUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            ReadyAutoResponseBatchService.class
    );

    private final GetReadyAutoResponseQueueItemsUseCase
            getReadyAutoResponseQueueItemsUseCase;
    private final ExecuteAutoResponseUseCase executeAutoResponseUseCase;
    private final AutoResponseBatchProgressStore progressStore;
    private final Executor batchExecutor;

    public ReadyAutoResponseBatchService(
            GetReadyAutoResponseQueueItemsUseCase
                    getReadyAutoResponseQueueItemsUseCase,
            ExecuteAutoResponseUseCase executeAutoResponseUseCase,
            AutoResponseBatchProgressStore progressStore,
            @Qualifier("autoResponseBatchExecutor")
            Executor batchExecutor
    ) {
        this.getReadyAutoResponseQueueItemsUseCase =
                Objects.requireNonNull(
                        getReadyAutoResponseQueueItemsUseCase,
                        "Get ready queue items use case must not be null"
                );
        this.executeAutoResponseUseCase = Objects.requireNonNull(
                executeAutoResponseUseCase,
                "Execute auto response use case must not be null"
        );
        this.progressStore = Objects.requireNonNull(
                progressStore,
                "Batch progress store must not be null"
        );
        this.batchExecutor = Objects.requireNonNull(
                batchExecutor,
                "Batch executor must not be null"
        );
    }

    @Override
    public CompletableFuture<StartReadyAutoResponsesBatchResultDto> start(
            StartReadyAutoResponsesBatchCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Start ready auto responses batch command must not be null"
        );

        return getReadyAutoResponseQueueItemsUseCase
                .getReadyItems(command.userId())
                .thenApply(readyItems ->
                        startForReadyItems(command.userId(), readyItems)
                );
    }

    @Override
    public Optional<AutoResponseBatchProgressDto> getProgress(
            UUID batchId
    ) {
        return progressStore.get(batchId);
    }

    private StartReadyAutoResponsesBatchResultDto startForReadyItems(
            UserId userId,
            List<AutoResponseQueueItemDto> readyItems
    ) {
        if (readyItems.isEmpty()) {
            return StartReadyAutoResponsesBatchResultDto.noReadyItems();
        }

        AutoResponseBatchProgressStore.BatchRegistration registration =
                progressStore.tryStart(userId, readyItems.size());

        if (!registration.started()) {
            return StartReadyAutoResponsesBatchResultDto.alreadyRunning(
                    registration.progress().batchId(),
                    registration.progress().plannedCount()
            );
        }

        UUID batchId = registration.progress().batchId();

        try {
            batchExecutor.execute(() -> executeBatch(
                    batchId,
                    userId,
                    List.copyOf(readyItems)
            ));
        } catch (RuntimeException exception) {
            progressStore.markWorkerFailure(batchId);
            progressStore.complete(batchId);

            log.error(
                    "Could not start auto response batch worker: "
                            + "batchId={}, userId={}",
                    batchId,
                    userId,
                    exception
            );

            return StartReadyAutoResponsesBatchResultDto.failedToStart(
                    batchId,
                    readyItems.size(),
                    "Не удалось запустить фоновый worker: "
                            + rootMessage(exception)
            );
        }

        log.info(
                "Ready auto response batch started: "
                        + "batchId={}, userId={}, plannedCount={}",
                batchId,
                userId,
                readyItems.size()
        );

        return StartReadyAutoResponsesBatchResultDto.started(
                batchId,
                readyItems.size()
        );
    }

    private void executeBatch(
            UUID batchId,
            UserId userId,
            List<AutoResponseQueueItemDto> readyItems
    ) {
        progressStore.markRunning(batchId);

        boolean habrStreamPaused = false;

        try {
            for (AutoResponseQueueItemDto item : readyItems) {
                if (habrStreamPaused
                        && item.source() == VacancySource.HABR_CAREER) {
                    progressStore.recordSkipped(batchId);

                    log.info(
                            "Habr Career batch item skipped after safety pause: "
                                    + "batchId={}, itemId={}, externalVacancyId={}",
                            batchId,
                            item.id(),
                            item.externalVacancyId()
                    );
                    continue;
                }

                boolean pauseHabrAfterItem = executeSingleItem(
                        batchId,
                        userId,
                        item
                );

                habrStreamPaused = habrStreamPaused || pauseHabrAfterItem;
            }
        } catch (RuntimeException exception) {
            progressStore.markWorkerFailure(batchId);

            log.error(
                    "Auto response batch worker stopped unexpectedly: "
                            + "batchId={}, userId={}",
                    batchId,
                    userId,
                    exception
            );
        } finally {
            progressStore.complete(batchId);

            log.info(
                    "Ready auto response batch completed: "
                            + "batchId={}, userId={}, progress={}",
                    batchId,
                    userId,
                    progressStore.get(batchId).orElse(null)
            );
        }
    }

    private boolean executeSingleItem(
            UUID batchId,
            UserId userId,
            AutoResponseQueueItemDto item
    ) {
        progressStore.markItemStarted(batchId);

        try {
            AutoResponseExecutionResultDto result =
                    executeAutoResponseUseCase.execute(
                            new ExecuteAutoResponseCommand(
                                    userId,
                                    AutoResponseQueueItemId.of(item.id())
                            )
                    ).join();

            recordExecutionResult(batchId, result);

            boolean pauseHabrStream = HabrCareerBatchContinuationPolicy
                    .shouldPauseHabrStream(item, result);

            if (pauseHabrStream) {
                String pauseReason = HabrCareerBatchContinuationPolicy
                        .pauseReason(result);

                progressStore.recordHabrStreamPaused(
                        batchId,
                        pauseReason
                );

                log.warn(
                        "Habr Career batch stream paused after "
                                + "non-confirmed result: batchId={}, itemId={}, "
                                + "externalVacancyId={}, status={}",
                        batchId,
                        item.id(),
                        item.externalVacancyId(),
                        result.status()
                );
            }

            log.info(
                    "Auto response batch item completed: "
                            + "batchId={}, itemId={}, externalVacancyId={}, "
                            + "status={}",
                    batchId,
                    item.id(),
                    item.externalVacancyId(),
                    result.status()
            );

            return pauseHabrStream;
        } catch (CompletionException exception) {
            Throwable cause = rootCause(exception);

            if (cause instanceof AutoResponseQueueItemNotReadyException) {
                progressStore.recordSkipped(batchId);

                log.info(
                        "Auto response batch item skipped because it is no "
                                + "longer READY: batchId={}, itemId={}, "
                                + "externalVacancyId={}",
                        batchId,
                        item.id(),
                        item.externalVacancyId()
                );

                return false;
            }

            progressStore.recordFailed(batchId);

            if (item.source() == VacancySource.HABR_CAREER) {
                progressStore.recordHabrStreamPaused(
                        batchId,
                        HabrCareerBatchContinuationPolicy
                                .pauseReasonAfterUnexpectedFailure()
                );
            }

            log.warn(
                    "Auto response batch item failed: "
                            + "batchId={}, itemId={}, externalVacancyId={}",
                    batchId,
                    item.id(),
                    item.externalVacancyId(),
                    cause
            );

            return item.source() == VacancySource.HABR_CAREER;
        }
    }

    private void recordExecutionResult(
            UUID batchId,
            AutoResponseExecutionResultDto result
    ) {
        switch (result.status()) {
            case SUCCESS, ALREADY_RESPONDED -> progressStore.recordSent(batchId);
            case PARTIAL_SUCCESS -> progressStore.recordPartialSuccess(batchId);
            case CANDIDATE_APPROVAL_REQUIRED ->
                    progressStore.recordCandidateApprovalRequired(batchId);
            case NOT_AVAILABLE,
                 PREFLIGHT_COMPLETED,
                 QUESTIONNAIRE_REQUIRED,
                 QUESTIONNAIRE_FILLED_REVIEW_REQUIRED ->
                    progressStore.recordReturnedToReady(batchId);
            case FAILED -> progressStore.recordFailed(batchId);
        }
    }

    private Throwable rootCause(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private String rootMessage(
            Throwable throwable
    ) {
        String message = rootCause(throwable).getMessage();

        return message == null || message.isBlank()
                ? rootCause(throwable).getClass().getSimpleName()
                : message;
    }
}
