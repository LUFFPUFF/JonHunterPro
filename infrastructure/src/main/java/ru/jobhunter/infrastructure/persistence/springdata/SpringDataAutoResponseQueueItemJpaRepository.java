package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jobhunter.infrastructure.persistence.entity.AutoResponseQueueItemEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
        update AutoResponseQueueItemEntity item
        set item.status = :inProgressStatus,
            item.updatedAt = :updatedAt
        where item.id = :itemId
          and item.userId = :userId
          and item.status = :readyStatus
        """)
    int claimReadyForExecution(
            @Param("itemId") UUID itemId,
            @Param("userId") UUID userId,
            @Param("readyStatus") String readyStatus,
            @Param("inProgressStatus") String inProgressStatus,
            @Param("updatedAt") Instant updatedAt
    );
}
