package ru.jobhunter.infrastructure.persistence.adapter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ru.jobhunter.core.domain.model.*;
import ru.jobhunter.core.domain.repository.AutoResponseQueueRepository;
import ru.jobhunter.infrastructure.persistence.entity.AutoResponseQueueItemEntity;
import ru.jobhunter.infrastructure.persistence.mapper.AutoResponseQueueItemPersistenceMapper;
import ru.jobhunter.infrastructure.persistence.springdata.SpringDataAutoResponseQueueItemJpaRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Repository
public class AutoResponseQueueRepositoryAdapter implements AutoResponseQueueRepository {

    private final SpringDataAutoResponseQueueItemJpaRepository jpaRepository;
    private final AutoResponseQueueItemPersistenceMapper mapper;
    private final ExecutorService executorService;

    public AutoResponseQueueRepositoryAdapter(
            SpringDataAutoResponseQueueItemJpaRepository jpaRepository,
            AutoResponseQueueItemPersistenceMapper mapper,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.jpaRepository = Objects.requireNonNull(
                jpaRepository,
                "Auto response queue JPA repository must not be null"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "Auto response queue persistence mapper must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Executor service must not be null"
        );
    }

    @Override
    public CompletableFuture<AutoResponseQueueItem> save(AutoResponseQueueItem item) {
        Objects.requireNonNull(item, "Auto response queue item must not be null");

        return CompletableFuture.supplyAsync(() -> {
            AutoResponseQueueItemEntity savedEntity = jpaRepository.save(mapper.toEntity(item));
            return mapper.toDomain(savedEntity);
        }, executorService);
    }

    @Override
    public CompletableFuture<Optional<AutoResponseQueueItem>> findByUserIdAndSourceAndExternalVacancyId(
            UserId userId,
            VacancySource source,
            String externalVacancyId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(source, "Vacancy source must not be null");

        if (externalVacancyId == null || externalVacancyId.isBlank()) {
            throw new IllegalArgumentException("External vacancy id must not be blank");
        }

        return CompletableFuture.supplyAsync(() ->
                        jpaRepository.findByUserIdAndSourceAndExternalVacancyId(
                                        userId.value(),
                                        source.code(),
                                        externalVacancyId.trim()
                                )
                                .map(mapper::toDomain),
                executorService
        );
    }

    @Override
    public CompletableFuture<List<AutoResponseQueueItem>> findByUserIdOrderByCreatedAtDesc(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return CompletableFuture.supplyAsync(() ->
                        jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId.value())
                                .stream()
                                .map(mapper::toDomain)
                                .toList(),
                executorService
        );
    }

    @Override
    public CompletableFuture<List<AutoResponseQueueItem>> findByUserIdAndStatusOrderByCreatedAtDesc(
            UserId userId,
            AutoResponseQueueStatus status
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(status, "Auto response queue status must not be null");

        return CompletableFuture.supplyAsync(() ->
                        jpaRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                                        userId.value(),
                                        status.name()
                                )
                                .stream()
                                .map(mapper::toDomain)
                                .toList(),
                executorService
        );
    }

    @Override
    public CompletableFuture<Optional<AutoResponseQueueItem>> findByIdAndUserId(
            AutoResponseQueueItemId itemId,
            UserId userId
    ) {
        Objects.requireNonNull(itemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");

        return CompletableFuture.supplyAsync(() ->
                        jpaRepository.findByIdAndUserId(
                                        itemId.value(),
                                        userId.value()
                                )
                                .map(mapper::toDomain),
                executorService
        );
    }

    @Override
    public CompletableFuture<Boolean> deleteByIdAndUserId(
            AutoResponseQueueItemId itemId,
            UserId userId
    ) {
        Objects.requireNonNull(itemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");

        return CompletableFuture.supplyAsync(() -> {
            Optional<AutoResponseQueueItemEntity> entity =
                    jpaRepository.findByIdAndUserId(
                            itemId.value(),
                            userId.value()
                    );

            if (entity.isEmpty()) {
                return false;
            }

            jpaRepository.delete(entity.get());
            return true;
        }, executorService);
    }

    @Override
    public CompletableFuture<Optional<AutoResponseQueueItem>> updateStatus(
            AutoResponseQueueItemId itemId,
            UserId userId,
            AutoResponseQueueStatus status
    ) {
        Objects.requireNonNull(itemId, "Auto response queue item id must not be null");
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(status, "Auto response queue status must not be null");

        return CompletableFuture.supplyAsync(() -> {
            Optional<AutoResponseQueueItemEntity> entity =
                    jpaRepository.findByIdAndUserId(
                            itemId.value(),
                            userId.value()
                    );

            if (entity.isEmpty()) {
                return Optional.empty();
            }

            AutoResponseQueueItem existingItem = mapper.toDomain(entity.get());
            AutoResponseQueueItem updatedItem = existingItem.withStatus(status);

            AutoResponseQueueItemEntity savedEntity = jpaRepository.save(
                    mapper.toEntity(updatedItem)
            );

            return Optional.of(mapper.toDomain(savedEntity));
        }, executorService);
    }
}
