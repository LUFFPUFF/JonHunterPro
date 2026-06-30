package ru.jobhunter.core.application.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HabrCareerVacancySearchQueryTest {

    @Test
    void normalizesOptionalSearchText() {
        HabrCareerVacancySearchQuery query =
                new HabrCareerVacancySearchQuery("  Java  ", 2);

        assertEquals("Java", query.query());
        assertEquals(2, query.page());
    }

    @Test
    void acceptsBlankSearchTextForNativeHabrDefaultList() {
        HabrCareerVacancySearchQuery query =
                new HabrCareerVacancySearchQuery(null, 1);

        assertEquals("", query.query());
    }

    @Test
    void rejectsZeroPage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HabrCareerVacancySearchQuery("Java", 0)
        );
    }
}
