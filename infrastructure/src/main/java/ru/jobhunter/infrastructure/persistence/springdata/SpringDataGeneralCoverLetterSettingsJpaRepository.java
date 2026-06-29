package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jobhunter.infrastructure.persistence.entity.GeneralCoverLetterSettingsEntity;

import java.util.UUID;

public interface SpringDataGeneralCoverLetterSettingsJpaRepository
        extends JpaRepository<GeneralCoverLetterSettingsEntity, UUID> {
}