package ru.jobhunter.core.application.dto;

public record HabrCareerVacancySearchProgressDto(
        int loadedPages,
        int totalPages,
        int loadedVacancyCount
) {

    public HabrCareerVacancySearchProgressDto {
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
        if (loadedVacancyCount < 0) {
            throw new IllegalArgumentException(
                    "Habr Career loaded vacancy count must not be negative"
            );
        }
    }

    public boolean hasKnownTotalPages() {
        return totalPages > 0;
    }
}
