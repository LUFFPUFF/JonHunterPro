package ru.jobhunter.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import ru.jobhunter.core.domain.model.CandidateQuestionnaireProfile;
import ru.jobhunter.core.domain.model.CandidateQuestionnaireProfileFacts;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.infrastructure.persistence.entity.CandidateQuestionnaireProfileEntity;

@Component
public class CandidateQuestionnaireProfilePersistenceMapper {

    public CandidateQuestionnaireProfileEntity toEntity(
            CandidateQuestionnaireProfile profile
    ) {
        CandidateQuestionnaireProfileFacts facts = profile.facts();

        CandidateQuestionnaireProfileEntity entity =
                new CandidateQuestionnaireProfileEntity();

        entity.setUserId(profile.userId().value());
        entity.setTimeZoneId(facts.timeZoneId());
        entity.setSalaryMin(facts.salaryMin());
        entity.setSalaryMax(facts.salaryMax());
        entity.setSalaryCurrency(facts.salaryCurrency());
        entity.setSalaryTaxBasis(facts.salaryTaxBasis());
        entity.setRelocationReady(facts.relocationReady());
        entity.setWorkFormatPreference(facts.workFormatPreference());
        entity.setRemoteWorkPriority(facts.remoteWorkPriority());
        entity.setEnglishLevel(facts.englishLevel());
        entity.setBusinessTripsReady(facts.businessTripsReady());
        entity.setTestAssignmentReadiness(facts.testAssignmentReadiness());
        entity.setStartAvailability(facts.startAvailability());
        entity.setAllowRelatedExperienceDrafts(
                facts.allowRelatedExperienceDrafts()
        );
        entity.setAdditionalConfirmedFacts(
                facts.additionalConfirmedFacts()
        );
        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());

        return entity;
    }

    public CandidateQuestionnaireProfile toDomain(
            CandidateQuestionnaireProfileEntity entity
    ) {
        CandidateQuestionnaireProfileFacts facts =
                new CandidateQuestionnaireProfileFacts(
                        entity.getTimeZoneId(),
                        entity.getSalaryMin(),
                        entity.getSalaryMax(),
                        entity.getSalaryCurrency(),
                        entity.getSalaryTaxBasis(),
                        entity.isRelocationReady(),
                        entity.getWorkFormatPreference(),
                        entity.isRemoteWorkPriority(),
                        entity.getEnglishLevel(),
                        entity.isBusinessTripsReady(),
                        entity.getTestAssignmentReadiness(),
                        entity.getStartAvailability(),
                        entity.isAllowRelatedExperienceDrafts(),
                        entity.getAdditionalConfirmedFacts()
                );

        return CandidateQuestionnaireProfile.restore(
                UserId.of(entity.getUserId()),
                facts,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}