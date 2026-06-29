package ru.jobhunter.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.jobhunter.infrastructure.persistence.entity.CandidateQuestionnaireProfileEntity;

import java.util.UUID;

public interface SpringDataCandidateQuestionnaireProfileJpaRepository
        extends JpaRepository<CandidateQuestionnaireProfileEntity, UUID> {
}