package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.*;

import java.math.BigDecimal;
import java.util.Objects;

public record SaveCandidateQuestionnaireProfileCommand(
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
        String additionalConfirmedFacts
) {

    public SaveCandidateQuestionnaireProfileCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(salaryMin, "Salary minimum must not be null");
        Objects.requireNonNull(salaryMax, "Salary maximum must not be null");
        Objects.requireNonNull(salaryTaxBasis, "Salary tax basis must not be null");
        Objects.requireNonNull(
                workFormatPreference,
                "Work format preference must not be null"
        );
        Objects.requireNonNull(
                testAssignmentReadiness,
                "Test assignment readiness must not be null"
        );
        Objects.requireNonNull(
                startAvailability,
                "Start availability must not be null"
        );
    }
}