package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.*;
import ru.jobhunter.core.application.usecase.autoresponse.*;
import ru.jobhunter.core.domain.model.AutoResponseQueueItem;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AutoResponseQueueService implements
        AddVacancyToAutoResponseQueueUseCase,
        GetAutoResponseQueueUseCase,
        GetReadyAutoResponseQueueItemsUseCase,
        RemoveAutoResponseQueueItemUseCase,
        UpdateAutoResponseQueueItemStatusUseCase
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
}
