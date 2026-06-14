package ru.jobhunter.core.application.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HhVacancySearchQueryTest {

    @Test
    void shouldNormalizeSearchQuery() {
        HhVacancySearchQuery query = new HhVacancySearchQuery(
                "  java developer  ",
                "  113  ",
                null,
                null
        );

        assertEquals("java developer", query.text());
        assertEquals("113", query.area());
        assertEquals(0, query.page());
        assertEquals(20, query.perPage());
    }

    @Test
    void shouldRejectNegativePage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new HhVacancySearchQuery(
                        "java",
                        "113",
                        -1,
                        20
                )
        );

        assertEquals("HH vacancy search page must not be negative", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidPerPage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new HhVacancySearchQuery(
                        "java",
                        "113",
                        0,
                        101
                )
        );

        assertEquals("HH vacancy search perPage must be between 1 and 100", exception.getMessage());
    }
}