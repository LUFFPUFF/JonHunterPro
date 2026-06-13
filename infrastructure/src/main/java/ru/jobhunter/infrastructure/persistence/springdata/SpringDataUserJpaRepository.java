package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jobhunter.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
