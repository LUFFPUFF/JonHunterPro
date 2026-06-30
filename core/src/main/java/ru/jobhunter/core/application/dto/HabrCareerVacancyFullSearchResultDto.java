package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record HabrCareerVacancyFullSearchResultDto(
        Status status,
        String finalUrl,
        List<HabrCareerVisibleVacancyDto> vacancies,
        int totalResults,
        int loadedPages,
        int totalPages,
        Integer failedPage,
        Instant capturedAt
) {

    public HabrCareerVacancyFullSearchResultDto {
        status = Objects.requireNonNull(
                status,
                "Habr Career full search status must not be null"
        );
        finalUrl = normalize(finalUrl);
        vacancies = List.copyOf(Objects.requireNonNullElse(vacancies, List.of()));

        if (totalResults < 0) {
            throw new IllegalArgumentException(
                    "Habr Career total result count must not be negative"
            );
        }
        if (loadedPages < 0) {
            throw new IllegalArgumentException(
                    "Habr Career loaded page count must not be negative"
            );
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException(
                    "Habr Career total page count must not be negative"
            );
        }
        if (totalPages > 0 && loadedPages > totalPages) {
            throw new IllegalArgumentException(
                    "Habr Career loaded page count must not exceed total page count"
            );
        }
        if (failedPage != null && failedPage < 1) {
            throw new IllegalArgumentException(
                    "Habr Career failed page number must be positive"
            );
        }

        capturedAt = Objects.requireNonNull(
                capturedAt,
                "Habr Career full search capture time must not be null"
        );
    }

    public boolean isReady() {
        return status == Status.ALL_PAGES_LOADED
                || status == Status.PARTIAL_PAGES_LOADED;
    }

    public boolean isComplete() {
        return status == Status.ALL_PAGES_LOADED;
    }

    public enum Status {
        ALL_PAGES_LOADED,
        PARTIAL_PAGES_LOADED,
        AUTHENTICATION_REQUIRED,
        VACANCY_CARDS_NOT_FOUND,
        UNEXPECTED_PAGE
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
