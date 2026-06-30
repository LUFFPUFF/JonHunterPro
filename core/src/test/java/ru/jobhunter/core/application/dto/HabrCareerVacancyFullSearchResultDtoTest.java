package ru.jobhunter.core.application.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabrCareerVacancyFullSearchResultDtoTest {

    @Test
    void completeResultIsReadyAndComplete() {
        HabrCareerVacancyFullSearchResultDto result =
                new HabrCareerVacancyFullSearchResultDto(
                        HabrCareerVacancyFullSearchResultDto.Status
                                .ALL_PAGES_LOADED,
                        "https://career.habr.com/vacancies?type=all&q=Java",
                        List.of(),
                        0,
                        0,
                        0,
                        null,
                        Instant.now()
                );

        assertTrue(result.isReady());
        assertTrue(result.isComplete());
    }

    @Test
    void partialResultIsReadyButNotComplete() {
        HabrCareerVacancyFullSearchResultDto result =
                new HabrCareerVacancyFullSearchResultDto(
                        HabrCareerVacancyFullSearchResultDto.Status
                                .PARTIAL_PAGES_LOADED,
                        "https://career.habr.com/vacancies?type=all&q=Java&page=3",
                        List.of(),
                        70,
                        2,
                        3,
                        3,
                        Instant.now()
                );

        assertTrue(result.isReady());
        assertFalse(result.isComplete());
    }
}
