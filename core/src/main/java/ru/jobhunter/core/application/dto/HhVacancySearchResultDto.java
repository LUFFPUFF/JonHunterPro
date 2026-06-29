package ru.jobhunter.core.application.dto;

import java.util.List;
import java.util.Objects;

public record HhVacancySearchResultDto(
        List<HhVacancyDto> vacancies,
        int found,
        int pages,
        int page,
        int perPage
) {

    public HhVacancySearchResultDto {
        vacancies = List.copyOf(
                Objects.requireNonNull(
                        vacancies,
                        "Vacancies must not be null"
                )
        );
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return pages > 0 && page < pages - 1;
    }

    public int displayedPageNumber() {
        return pages <= 0 ? 0 : page + 1;
    }
}