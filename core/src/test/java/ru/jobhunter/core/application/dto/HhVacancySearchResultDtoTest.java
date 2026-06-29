package ru.jobhunter.core.application.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HhVacancySearchResultDtoTest {

    @Test
    void shouldExposeNextAndPreviousPageForMiddlePage() {
        HhVacancySearchResultDto result =
                new HhVacancySearchResultDto(
                        List.of(),
                        80,
                        4,
                        1,
                        20
                );

        assertTrue(result.hasPreviousPage());
        assertTrue(result.hasNextPage());
        assertEquals(2, result.displayedPageNumber());
    }

    @Test
    void shouldNotExposePreviousPageForFirstPage() {
        HhVacancySearchResultDto result =
                new HhVacancySearchResultDto(
                        List.of(),
                        80,
                        4,
                        0,
                        20
                );

        assertFalse(result.hasPreviousPage());
        assertTrue(result.hasNextPage());
        assertEquals(1, result.displayedPageNumber());
    }

    @Test
    void shouldNotExposeNextPageForLastPage() {
        HhVacancySearchResultDto result =
                new HhVacancySearchResultDto(
                        List.of(),
                        80,
                        4,
                        3,
                        20
                );

        assertTrue(result.hasPreviousPage());
        assertFalse(result.hasNextPage());
        assertEquals(4, result.displayedPageNumber());
    }
}