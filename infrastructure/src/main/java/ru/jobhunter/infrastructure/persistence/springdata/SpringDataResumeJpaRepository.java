package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.jobhunter.infrastructure.persistence.entity.ResumeEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataResumeJpaRepository
        extends JpaRepository<ResumeEntity, UUID> {

    Optional<ResumeEntity> findByUserIdAndPrimaryResumeTrue(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ResumeEntity resume
            set resume.primaryResume = false,
                resume.updatedAt = :updatedAt
            where resume.userId = :userId
              and resume.primaryResume = true
            """)
    int clearPrimaryResumeByUserId(
            @Param("userId") UUID userId,
            @Param("updatedAt") Instant updatedAt
    );
}