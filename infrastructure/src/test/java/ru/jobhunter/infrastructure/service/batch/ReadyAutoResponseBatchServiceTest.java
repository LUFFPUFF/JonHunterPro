package ru.jobhunter.infrastructure.service.batch;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.jobhunter.core.application.dto.AutoResponseBatchProgressStatus;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.AutoResponseQueueItemDto;
import ru.jobhunter.core.application.dto.ExecuteAutoResponseCommand;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                VacancySource.HH_RU,
                "1001",
                "Java Developer"
        );

        AutoResponseQueueItemDto secondItem = readyItem(
                VacancySource.HH_RU,
                "1002",
                "Backend Developer"
        );

        GetReadyAutoResponseQueueItemsUseCase readyItemsUseCase =
                mock(GetReadyAutoResponseQueueItemsUseCase.class);

        ExecuteAutoResponseUseCase executionUseCase =
                mock(ExecuteAutoResponseUseCase.class);

        when(readyItemsUseCase.getReadyItems(userId)).thenReturn(
                CompletableFuture.completedFuture(List.of(firstItem, secondItem))
        );

        when(executionUseCase.execute(any())).thenReturn(
                CompletableFuture.completedFuture(
                        successFor(firstItem)
                ),
                CompletableFuture.completedFuture(
                        successFor(secondItem)
                )
        );

        ReadyAutoResponseBatchService service = newService(
                readyItemsUseCase,
                executionUseCase
        );

        StartReadyAutoResponsesBatchResultDto result = service.start(
                new StartReadyAutoResponsesBatchCommand(userId)
        ).join();

        var progress = service.getProgress(result.batchId()).orElseThrow();

        assertEquals(AutoResponseBatchProgressStatus.COMPLETED, progress.status());
        assertEquals(2, progress.sentCount());
        assertEquals(0, progress.failedCount());
        assertEquals(0, progress.skippedCount());
        assertEquals(null, progress.habrStreamPauseReason());

        verify(executionUseCase, times(2)).execute(any());
    }

    @Test
    void shouldRecordPartialSuccessSeparatelyFromConfirmedSentResponse() {
        UserId userId = UserId.of(UUID.randomUUID());
        AutoResponseQueueItemDto item = readyItem(
                VacancySource.HH_RU,
                "1003",
                "Java Backend Developer"
        );

        GetReadyAutoResponseQueueItemsUseCase readyItemsUseCase =
                mock(GetReadyAutoResponseQueueItemsUseCase.class);
        ExecuteAutoResponseUseCase executionUseCase =
                mock(ExecuteAutoResponseUseCase.class);

        when(readyItemsUseCase.getReadyItems(userId)).thenReturn(
                CompletableFuture.completedFuture(List.of(item))
        );

        when(executionUseCase.execute(any())).thenReturn(
                CompletableFuture.completedFuture(
                        AutoResponseExecutionResultDto.partialSuccess(
                                AutoResponseQueueItemId.of(item.id()),
                                VacancySource.HH_RU,
                                item.externalVacancyId(),
                                "HH sent the resume without confirmed letter"
                        )
                )
        );

        ReadyAutoResponseBatchService service = newService(
                readyItemsUseCase,
                executionUseCase
        );

        StartReadyAutoResponsesBatchResultDto result = service.start(
                new StartReadyAutoResponsesBatchCommand(userId)
        ).join();

        var progress = service.getProgress(result.batchId()).orElseThrow();

        assertEquals(
                AutoResponseBatchProgressStatus.COMPLETED_WITH_ISSUES,
                progress.status()
        );
        assertEquals(0, progress.sentCount());
        assertEquals(1, progress.partialSuccessCount());
        assertEquals(0, progress.returnedToReadyCount());
        assertEquals(0, progress.failedCount());
        assertEquals(null, progress.habrStreamPauseReason());
    }

    @Test
    void shouldPauseOnlyHabrStreamAfterPartialSuccessAndContinueHhItems() {
        UserId userId = UserId.of(UUID.randomUUID());

        AutoResponseQueueItemDto firstHabrItem = readyItem(
                VacancySource.HABR_CAREER,
                "habr-1001",
                "Habr Java Developer"
        );

        AutoResponseQueueItemDto secondHabrItem = readyItem(
                VacancySource.HABR_CAREER,
                "habr-1002",
                "Habr Backend Developer"
        );

        AutoResponseQueueItemDto hhItem = readyItem(
                VacancySource.HH_RU,
                "hh-1003",
                "HH Java Developer"
        );

        GetReadyAutoResponseQueueItemsUseCase readyItemsUseCase =
                mock(GetReadyAutoResponseQueueItemsUseCase.class);
        ExecuteAutoResponseUseCase executionUseCase =
                mock(ExecuteAutoResponseUseCase.class);

        when(readyItemsUseCase.getReadyItems(userId)).thenReturn(
                CompletableFuture.completedFuture(
                        List.of(firstHabrItem, secondHabrItem, hhItem)
                )
        );

        when(executionUseCase.execute(any())).thenAnswer(invocation -> {
            ExecuteAutoResponseCommand command = invocation.getArgument(0);

            if (command.queueItemId().equals(
                    AutoResponseQueueItemId.of(firstHabrItem.id())
            )) {
                return CompletableFuture.completedFuture(
                        AutoResponseExecutionResultDto.partialSuccess(
                                AutoResponseQueueItemId.of(firstHabrItem.id()),
                                VacancySource.HABR_CAREER,
                                firstHabrItem.externalVacancyId(),
                                "Habr response was created without verified letter"
                        )
                );
            }

            if (command.queueItemId().equals(
                    AutoResponseQueueItemId.of(hhItem.id())
            )) {
                return CompletableFuture.completedFuture(successFor(hhItem));
            }

            throw new AssertionError(
                    "Second Habr item must not be executed after pause"
            );
        });

        ReadyAutoResponseBatchService service = newService(
                readyItemsUseCase,
                executionUseCase
        );

        StartReadyAutoResponsesBatchResultDto result = service.start(
                new StartReadyAutoResponsesBatchCommand(userId)
        ).join();

        var progress = service.getProgress(result.batchId()).orElseThrow();

        assertEquals(
                AutoResponseBatchProgressStatus.COMPLETED_WITH_ISSUES,
                progress.status()
        );
        assertEquals(3, progress.plannedCount());
        assertEquals(2, progress.startedCount());
        assertEquals(1, progress.sentCount());
        assertEquals(1, progress.partialSuccessCount());
        assertEquals(1, progress.skippedCount());
        assertNotNull(progress.habrStreamPauseReason());

        ArgumentCaptor<ExecuteAutoResponseCommand> commandCaptor =
                ArgumentCaptor.forClass(ExecuteAutoResponseCommand.class);

        verify(executionUseCase, times(2)).execute(commandCaptor.capture());

        assertEquals(
                List.of(
                        AutoResponseQueueItemId.of(firstHabrItem.id()),
                        AutoResponseQueueItemId.of(hhItem.id())
                ),
                commandCaptor.getAllValues()
                        .stream()
                        .map(ExecuteAutoResponseCommand::queueItemId)
                        .toList()
        );
    }

    private ReadyAutoResponseBatchService newService(
            GetReadyAutoResponseQueueItemsUseCase readyItemsUseCase,
            ExecuteAutoResponseUseCase executionUseCase
    ) {
        return new ReadyAutoResponseBatchService(
                readyItemsUseCase,
                executionUseCase,
                new AutoResponseBatchProgressStore(),
                Runnable::run
        );
    }

    private AutoResponseExecutionResultDto successFor(
            AutoResponseQueueItemDto item
    ) {
        return AutoResponseExecutionResultDto.success(
                AutoResponseQueueItemId.of(item.id()),
                item.source(),
                item.externalVacancyId(),
                "Отклик отправлен"
        );
    }

    private AutoResponseQueueItemDto readyItem(
            VacancySource source,
            String externalVacancyId,
            String vacancyName
    ) {
        Instant now = Instant.parse("2026-06-27T12:00:00Z");

        return new AutoResponseQueueItemDto(
                UUID.randomUUID(),
                source,
                externalVacancyId,
                vacancyName,
                "Example Company",
                "Москва",
                "https://example.test/vacancy/" + externalVacancyId,
                AutoResponseQueueStatus.READY,
                now,
                now
        );
    }
}
