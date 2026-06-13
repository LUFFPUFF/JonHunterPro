package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jobhunter.infrastructure.persistence.entity.ExternalAuthTokenEntity;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataExternalAuthTokenJpaRepository
        extends JpaRepository<ExternalAuthTokenEntity, UUID> {

    Optional<ExternalAuthTokenEntity> findByUserIdAndProvider(UUID userId, String provider);

    void deleteByUserIdAndProvider(UUID userId, String provider);
}
