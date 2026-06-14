package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jobhunter.infrastructure.persistence.entity.AutoResponseQueueItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAutoResponseQueueItemJpaRepository
        extends JpaRepository<AutoResponseQueueItemEntity, UUID> {

    Optional<AutoResponseQueueItemEntity> findByUserIdAndSourceAndExternalVacancyId(
            UUID userId,
            String source,
            String externalVacancyId
    );

    List<AutoResponseQueueItemEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AutoResponseQueueItemEntity> findAllByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId,
            String status
    );

    Optional<AutoResponseQueueItemEntity> findByIdAndUserId(UUID id, UUID userId);
}
