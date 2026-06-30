package ru.jobhunter.infrastructure.platform.habr.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchQuery;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HabrCareerVacancySearchServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsNativeHabrSearchUrlWithQueryAndPage() {
        String url = HabrCareerVacancySearchService.buildSearchUrl(
                new HabrCareerVacancySearchQuery("Java Spring", 2)
        );

        assertEquals(
                "https://career.habr.com/vacancies?type=all&q=Java%20Spring&page=2",
                url
        );
    }

    @Test
    void keepsFirstPageWithoutPageParameter() {
        String url = HabrCareerVacancySearchService.buildSearchUrl(
                new HabrCareerVacancySearchQuery("Java", 1)
        );

        assertEquals(
                "https://career.habr.com/vacancies?type=all&q=Java",
                url
        );
    }

    @Test
    void parsesNativeHabrPaginationMetadataFromSsrState() {
        HabrCareerVacancySearchService.SearchMetadata metadata =
                HabrCareerVacancySearchService.parseSearchMetadata(
                        """
                        {
                          "vacancies": {
                            "meta": {
                              "totalResults": 1148,
                              "perPage": 25,
                              "currentPage": 2,
                              "totalPages": 37
                            }
                          }
                        }
                        """,
                        objectMapper,
                        2,
                        25
                );

        assertEquals(1148, metadata.totalResults());
        assertEquals(25, metadata.perPage());
        assertEquals(2, metadata.currentPage());
        assertEquals(37, metadata.totalPages());
    }

    @Test
    void treatsEmptyNativeSearchPageAsReady() {
        HabrCareerVacancySearchResultDto.Status status =
                HabrCareerVacancySearchService.resolveStatus(
                        "https://career.habr.com/vacancies?type=all&q=unknown",
                        0,
                        0
                );

        assertEquals(
                HabrCareerVacancySearchResultDto.Status.VACANCIES_READY,
                status
        );
    }
}
