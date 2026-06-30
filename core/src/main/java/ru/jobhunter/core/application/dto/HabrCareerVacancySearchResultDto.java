package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record HabrCareerVacancySearchResultDto(
        Status status,
        String finalUrl,
        List<HabrCareerVisibleVacancyDto> vacancies,
        int totalResults,
        int perPage,
        int currentPage,
        int totalPages,
        Instant capturedAt
) {

    public HabrCareerVacancySearchResultDto {
        status = Objects.requireNonNull(
                status,
                "Habr Career vacancy search status must not be null"
        );
        finalUrl = normalize(finalUrl);
        vacancies = List.copyOf(Objects.requireNonNullElse(vacancies, List.of()));

        if (totalResults < 0) {
            throw new IllegalArgumentException(
                    "Habr Career total result count must not be negative"
            );
        }
        if (perPage < 0) {
            throw new IllegalArgumentException(
                    "Habr Career page size must not be negative"
            );
        }
        if (currentPage < 0) {
            throw new IllegalArgumentException(
                    "Habr Career current page must not be negative"
            );
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException(
                    "Habr Career page count must not be negative"
            );
        }
        if (totalPages > 0 && currentPage > totalPages) {
            throw new IllegalArgumentException(
                    "Habr Career current page must not exceed total pages"
            );
        }

        capturedAt = Objects.requireNonNull(
                capturedAt,
                "Habr Career vacancy search capture time must not be null"
        );
    }

    public boolean isReady() {
        return status == Status.VACANCIES_READY;
    }

    public boolean hasPreviousPage() {
        return isReady() && currentPage > 1;
    }

    public boolean hasNextPage() {
        return isReady() && totalPages > 0 && currentPage < totalPages;
    }

    public enum Status {
        VACANCIES_READY,
        AUTHENTICATION_REQUIRED,
        VACANCY_CARDS_NOT_FOUND,
        UNEXPECTED_PAGE
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
