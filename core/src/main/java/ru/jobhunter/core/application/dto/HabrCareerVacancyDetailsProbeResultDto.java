package ru.jobhunter.core.application.dto;

import java.time.Instant;

public record HabrCareerVacancyDetailsProbeResultDto(
        Status status,
        String requestedExternalVacancyId,
        String finalUrl,
        String pageTitle,
        HabrCareerVacancyDetailsDto vacancy,
        String responseActionSelector,
        String diagnosticDirectory,
        Instant capturedAt
) {

    public enum Status {
        VACANCY_DETAILS_READY,
        AUTHENTICATION_REQUIRED,
        VACANCY_DETAILS_NOT_FOUND,
        UNEXPECTED_PAGE
    }
}
