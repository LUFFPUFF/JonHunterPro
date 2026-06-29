package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.MarkAutoResponseQueueItemsReadyCommand;
import ru.jobhunter.core.application.dto.MarkAutoResponseQueueItemsReadyResultDto;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoResponseQueueServiceBatchReadyTest {

    @Test
    void shouldMarkOnlyQueuedItemsAsReady() {
        AutoResponseQueueRepository repository =
                mock(AutoResponseQueueRepository.class);

        AutoResponseQueueService service =
                new AutoResponseQueueService(repository);

        UserId userId = UserId.of(UUID.randomUUID());

        AutoResponseQueueItem queuedItem = queueItem(
                userId,
                AutoResponseQueueStatus.QUEUED
        );

        AutoResponseQueueItem readyItem = queueItem(
                userId,
                AutoResponseQueueStatus.READY
        );

        when(repository.findByIdAndUserId(
                eq(queuedItem.id()),
                eq(userId)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        Optional.of(queuedItem)
                )
        );

        when(repository.findByIdAndUserId(
                eq(readyItem.id()),
                eq(userId)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        Optional.of(readyItem)
                )
        );

        when(repository.updateStatus(
                eq(queuedItem.id()),
                eq(userId),
                eq(AutoResponseQueueStatus.READY)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        Optional.of(
                                queuedItem.withStatus(
                                        AutoResponseQueueStatus.READY
                                )
                        )
                )
        );

        MarkAutoResponseQueueItemsReadyResultDto result =
                service.markReady(
                        new MarkAutoResponseQueueItemsReadyCommand(
                                userId,
                                List.of(
                                        queuedItem.id(),
                                        readyItem.id()
                                )
                        )
                ).join();

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.markedReadyCount());
        assertEquals(1, result.alreadyReadyCount());
        assertEquals(0, result.notEligibleCount());
        assertEquals(0, result.notFoundCount());
        assertEquals(0, result.failedCount());
    }

    private AutoResponseQueueItem queueItem(
            UserId userId,
            AutoResponseQueueStatus status
    ) {
        AutoResponseQueueItem item = AutoResponseQueueItem.create(
                userId,
                VacancySource.HH_RU,
                UUID.randomUUID().toString(),
                "Java Developer",
                "Example Company",
                "Москва",
                "https://hh.ru/vacancy/1"
        );

        return item.withStatus(status);
    }
}