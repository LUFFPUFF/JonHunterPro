package ru.jobhunter.infrastructure.service.batch;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.AutoResponseBatchProgressStatus;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.application.dto.StartReadyAutoResponsesBatchCommand;
import ru.jobhunter.core.application.dto.StartReadyAutoResponsesBatchResultDto;
import ru.jobhunter.core.application.usecase.autoresponse.ExecuteAutoResponseUseCase;
import ru.jobhunter.core.application.usecase.autoresponse.GetReadyAutoResponseQueueItemsUseCase;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadyAutoResponseBatchServiceTest {

    @Test
    void shouldExecuteAllReadyItemsSequentially() {
        UserId userId = UserId.of(UUID.randomUUID());

        AutoResponseQueueItemDto firstItem = readyItem(
                "1001",
                "Java Developer"
        );

        AutoResponseQueueItemDto secondItem = readyItem(
                "1002",
                "Backend Developer"
        );

        GetReadyAutoResponseQueueItemsUseCase readyItemsUseCase =
                mock(GetReadyAutoResponseQueueItemsUseCase.class);

        ExecuteAutoResponseUseCase executionUseCase =
                mock(ExecuteAutoResponseUseCase.class);

        when(readyItemsUseCase.getReadyItems(userId)).thenReturn(
                CompletableFuture.completedFuture(
                        List.of(firstItem, secondItem)
                )
        );

        when(executionUseCase.execute(any())).thenReturn(
                CompletableFuture.completedFuture(
                        AutoResponseExecutionResultDto.success(
                                AutoResponseQueueItemId.of(firstItem.id()),
                                VacancySource.HH_RU,
                                firstItem.externalVacancyId(),
                                "Отклик отправлен"
                        )
                ),
                CompletableFuture.completedFuture(
                        AutoResponseExecutionResultDto.success(
                                AutoResponseQueueItemId.of(secondItem.id()),
                                VacancySource.HH_RU,
                                secondItem.externalVacancyId(),
                                "Отклик отправлен"
                        )
                )
        );

        AutoResponseBatchProgressStore progressStore =
                new AutoResponseBatchProgressStore();

        ReadyAutoResponseBatchService service =
                new ReadyAutoResponseBatchService(
                        readyItemsUseCase,
                        executionUseCase,
                        progressStore,
                        Runnable::run
                );

        StartReadyAutoResponsesBatchResultDto result =
                service.start(
                        new StartReadyAutoResponsesBatchCommand(
                                userId
                        )
                ).join();

        assertEquals(
                2,
                result.plannedCount()
        );

        var progress = service.getProgress(
                result.batchId()
        ).orElseThrow();

        assertEquals(
                AutoResponseBatchProgressStatus.COMPLETED,
                progress.status()
        );

        assertEquals(2, progress.sentCount());
        assertEquals(0, progress.failedCount());
        assertEquals(0, progress.skippedCount());

        verify(executionUseCase, times(2))
                .execute(any());
    }

    @Test
    void shouldRecordPartialSuccessSeparatelyFromConfirmedSentResponse() {
        UserId userId = UserId.of(UUID.randomUUID());
        AutoResponseQueueItemDto item = readyItem("1003", "Java Backend Developer");
        GetReadyAutoResponseQueueItemsUseCase readyItemsUseCase = mock(GetReadyAutoResponseQueueItemsUseCase.class);
        ExecuteAutoResponseUseCase executionUseCase = mock(ExecuteAutoResponseUseCase.class);
        when(readyItemsUseCase.getReadyItems(userId)).thenReturn(CompletableFuture.completedFuture(List.of(item)));
        when(executionUseCase.execute(any())).thenReturn(CompletableFuture.completedFuture(AutoResponseExecutionResultDto.partialSuccess(AutoResponseQueueItemId.of(item.id()), VacancySource.HH_RU, item.externalVacancyId(), "HH sent the resume without confirmed letter")));
        AutoResponseBatchProgressStore progressStore = new AutoResponseBatchProgressStore();
        ReadyAutoResponseBatchService service = new ReadyAutoResponseBatchService(readyItemsUseCase, executionUseCase, progressStore, Runnable::run);
        StartReadyAutoResponsesBatchResultDto result = service.start(new StartReadyAutoResponsesBatchCommand(userId)).join();
        var progress = service.getProgress(result.batchId()).orElseThrow();
        assertEquals(AutoResponseBatchProgressStatus.COMPLETED_WITH_ISSUES, progress.status());
        assertEquals(0, progress.sentCount());
        assertEquals(1, progress.partialSuccessCount());
        assertEquals(0, progress.returnedToReadyCount());
        assertEquals(0, progress.failedCount());
    }

    private AutoResponseQueueItemDto readyItem(
            String externalVacancyId,
            String vacancyName
    ) {
        Instant now = Instant.parse(
                "2026-06-27T12:00:00Z"
        );

        return new AutoResponseQueueItemDto(
                UUID.randomUUID(),
                VacancySource.HH_RU,
                externalVacancyId,
                vacancyName,
                "Example Company",
                "Москва",
                "https://hh.ru/vacancy/" + externalVacancyId,
                AutoResponseQueueStatus.READY,
                now,
                now
        );
    }
}