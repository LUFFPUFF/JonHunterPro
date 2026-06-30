package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.ReturnAutoResponseQueueItemsToQueuedCommand;
import ru.jobhunter.core.application.dto.ReturnAutoResponseQueueItemsToQueuedResultDto;
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

class AutoResponseQueueServiceBatchReturnToQueuedTest {

    @Test
    void shouldReturnOnlyReadyItemsToQueued() {
        AutoResponseQueueRepository repository =
                mock(AutoResponseQueueRepository.class);

        AutoResponseQueueService service =
                new AutoResponseQueueService(repository);

        UserId userId = UserId.of(UUID.randomUUID());

        AutoResponseQueueItem readyItem = queueItem(
                userId,
                AutoResponseQueueStatus.READY
        );

        AutoResponseQueueItem queuedItem = queueItem(
                userId,
                AutoResponseQueueStatus.QUEUED
        );

        AutoResponseQueueItem sentItem = queueItem(
                userId,
                AutoResponseQueueStatus.SENT
        );

        when(repository.findByIdAndUserId(
                eq(readyItem.id()),
                eq(userId)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        Optional.of(readyItem)
                )
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
                eq(sentItem.id()),
                eq(userId)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        Optional.of(sentItem)
                )
        );

        when(repository.updateStatus(
                eq(readyItem.id()),
                eq(userId),
                eq(AutoResponseQueueStatus.QUEUED)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        Optional.of(
                                readyItem.withStatus(
                                        AutoResponseQueueStatus.QUEUED
                                )
                        )
                )
        );

        ReturnAutoResponseQueueItemsToQueuedResultDto result =
                service.returnToQueued(
                        new ReturnAutoResponseQueueItemsToQueuedCommand(
                                userId,
                                List.of(
                                        readyItem.id(),
                                        queuedItem.id(),
                                        sentItem.id()
                                )
                        )
                ).join();

        assertEquals(3, result.requestedCount());
        assertEquals(1, result.returnedToQueuedCount());
        assertEquals(1, result.alreadyQueuedCount());
        assertEquals(1, result.notEligibleCount());
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
