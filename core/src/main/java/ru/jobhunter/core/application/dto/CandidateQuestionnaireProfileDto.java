package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record CandidateQuestionnaireProfileDto(
        UserId userId,
        String timeZoneId,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        CandidateSalaryTaxBasis salaryTaxBasis,
        boolean relocationReady,
        CandidateWorkFormatPreference workFormatPreference,
        boolean remoteWorkPriority,
        String englishLevel,
        boolean businessTripsReady,
        CandidateTestAssignmentReadiness testAssignmentReadiness,
        CandidateStartAvailability startAvailability,
        boolean allowRelatedExperienceDrafts,
        String additionalConfirmedFacts,
        Instant createdAt,
        Instant updatedAt
) {

    public CandidateQuestionnaireProfileDto {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(timeZoneId, "Time zone must not be null");
        Objects.requireNonNull(salaryMin, "Salary minimum must not be null");
        Objects.requireNonNull(salaryMax, "Salary maximum must not be null");
        Objects.requireNonNull(salaryCurrency, "Salary currency must not be null");
        Objects.requireNonNull(salaryTaxBasis, "Salary tax basis must not be null");
        Objects.requireNonNull(workFormatPreference, "Work format preference must not be null");
        Objects.requireNonNull(englishLevel, "English level must not be null");
        Objects.requireNonNull(startAvailability, "Start availability must not be null");
        Objects.requireNonNull(additionalConfirmedFacts, "Additional confirmed facts must not be null");
        Objects.requireNonNull(createdAt, "Created at must not be null");
        Objects.requireNonNull(updatedAt, "Updated at must not be null");
        Objects.requireNonNull(testAssignmentReadiness, "Test assignment readiness must not be null");
    }

    public static CandidateQuestionnaireProfileDto from(
            CandidateQuestionnaireProfile profile
    ) {
        Objects.requireNonNull(profile, "Candidate questionnaire profile must not be null");

        var facts = profile.facts();

        return new CandidateQuestionnaireProfileDto(
                profile.userId(),
                facts.timeZoneId(),
                facts.salaryMin(),
                facts.salaryMax(),
                facts.salaryCurrency(),
                facts.salaryTaxBasis(),
                facts.relocationReady(),
                facts.workFormatPreference(),
                facts.remoteWorkPriority(),
                facts.englishLevel(),
                facts.businessTripsReady(),
                facts.testAssignmentReadiness(),
                facts.startAvailability(),
                facts.allowRelatedExperienceDrafts(),
                facts.additionalConfirmedFacts(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }
}