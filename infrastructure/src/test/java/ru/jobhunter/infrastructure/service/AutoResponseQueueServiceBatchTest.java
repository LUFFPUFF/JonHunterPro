package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.AddVacanciesToAutoResponseQueueCommand;
import ru.jobhunter.core.application.dto.AddVacanciesToAutoResponseQueueResultDto;
import ru.jobhunter.core.application.dto.AddVacancyToAutoResponseQueueCommand;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoResponseQueueServiceBatchTest {

    @Test
    void shouldAddNewVacanciesAndSkipExistingOnes() {
        AutoResponseQueueRepository repository =
                mock(AutoResponseQueueRepository.class);

        AutoResponseQueueService service =
                new AutoResponseQueueService(repository);

        UserId userId = UserId.of(UUID.randomUUID());

        AddVacancyToAutoResponseQueueCommand firstCommand =
                command(userId, "1001", "Java Developer");

        AddVacancyToAutoResponseQueueCommand secondCommand =
                command(userId, "1002", "Backend Developer");

        AutoResponseQueueItem existingItem =
                AutoResponseQueueItem.create(
                        userId,
                        VacancySource.HH_RU,
                        "1002",
                        "Backend Developer",
                        "Company",
                        "Москва",
                        "https://example.com/1002"
                );

        when(repository.findByUserIdAndSourceAndExternalVacancyId(
                eq(userId),
                eq(VacancySource.HH_RU),
                anyString()
        )).thenReturn(
                CompletableFuture.completedFuture(Optional.empty()),
                CompletableFuture.completedFuture(
                        Optional.of(existingItem)
                )
        );

        when(repository.save(any(AutoResponseQueueItem.class)))
                .thenAnswer(invocation ->
                        CompletableFuture.completedFuture(
                                invocation.getArgument(0)
                        )
                );

        AddVacanciesToAutoResponseQueueResultDto result =
                service.addAllToQueue(
                        new AddVacanciesToAutoResponseQueueCommand(
                                userId,
                                List.of(
                                        firstCommand,
                                        secondCommand
                                )
                        )
                ).join();

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.addedCount());
        assertEquals(1, result.alreadyExistsCount());
        assertEquals(0, result.failedCount());
    }

    private AddVacancyToAutoResponseQueueCommand command(
            UserId userId,
            String externalVacancyId,
            String vacancyName
    ) {
        return new AddVacancyToAutoResponseQueueCommand(
                userId,
                VacancySource.HH_RU,
                externalVacancyId,
                vacancyName,
                "Company",
                "Москва",
                "https://example.com/" + externalVacancyId
        );
    }
}