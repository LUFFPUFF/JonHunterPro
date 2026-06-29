package ru.jobhunter.core.domain.model;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

public record CandidateQuestionnaireProfileFacts(
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

    private static final int MAX_TIME_ZONE_LENGTH = 64;
    private static final int MAX_CURRENCY_LENGTH = 3;
    private static final int MAX_ENGLISH_LEVEL_LENGTH = 32;

    public CandidateQuestionnaireProfileFacts {
        timeZoneId = requireNonBlank(
                timeZoneId,
                "Time zone must not be blank",
                MAX_TIME_ZONE_LENGTH
        );
        validateTimeZone(timeZoneId);

        salaryMin = requireNonNegative(
                salaryMin,
                "Salary minimum must not be negative"
        );
        salaryMax = requireNonNegative(
                salaryMax,
                "Salary maximum must not be negative"
        );

        if (salaryMax.compareTo(salaryMin) < 0) {
            throw new IllegalArgumentException(
                    "Salary maximum must not be less than salary minimum"
            );
        }

        salaryCurrency = requireNonBlank(
                salaryCurrency,
                "Salary currency must not be blank",
                MAX_CURRENCY_LENGTH
        ).toUpperCase(Locale.ROOT);

        if (!salaryCurrency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Salary currency must contain exactly three Latin letters"
            );
        }

        salaryTaxBasis = Objects.requireNonNull(
                salaryTaxBasis,
                "Salary tax basis must not be null"
        );

        workFormatPreference = Objects.requireNonNull(
                workFormatPreference,
                "Work format preference must not be null"
        );

        englishLevel = requireNonBlank(
                englishLevel,
                "English level must not be blank",
                MAX_ENGLISH_LEVEL_LENGTH
        );

        testAssignmentReadiness = Objects.requireNonNull(
                testAssignmentReadiness,
                "Test assignment readiness must not be null"
        );

        startAvailability = Objects.requireNonNull(
                startAvailability,
                "Start availability must not be null"
        );

        additionalConfirmedFacts = normalizeOptional(additionalConfirmedFacts);
    }

    private static BigDecimal requireNonNegative(
            BigDecimal value,
            String message
    ) {
        Objects.requireNonNull(value, message);

        if (value.signum() < 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String requireNonBlank(
            String value,
            String message,
            int maxLength
    ) {
        String normalizedValue = normalizeOptional(value);

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(
                    message + ": maximum length is " + maxLength
            );
        }

        return normalizedValue;
    }

    private static void validateTimeZone(String timeZoneId) {
        try {
            ZoneId.of(timeZoneId);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "Unsupported time zone: " + timeZoneId,
                    exception
            );
        }
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}