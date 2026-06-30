package ru.jobhunter.core.application.dto;

import java.time.Instant;

public record HabrCareerVacancyPageProbeResultDto(
        Status status,
        String finalUrl,
        String pageTitle,
        int vacancyCardCount,
        String vacancyCardSelector,
        String diagnosticDirectory,
        Instant capturedAt
) {

    public enum Status {
        VACANCY_LIST_READY,
        AUTHENTICATION_REQUIRED,
        VACANCY_CARDS_NOT_FOUND,
        UNEXPECTED_PAGE
    }
}
