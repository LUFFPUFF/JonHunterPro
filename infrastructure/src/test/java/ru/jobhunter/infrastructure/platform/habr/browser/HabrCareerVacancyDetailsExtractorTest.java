package ru.jobhunter.infrastructure.platform.habr.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsProbeResultDto;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabrCareerVacancyDetailsExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsTrustedVacancyUrlFromNumericExternalId() {
        assertEquals(
                "https://career.habr.com/vacancies/1000167360",
                HabrCareerVacancyDetailsExtractor.buildVacancyUrl("1000167360")
        );
    }

    @Test
    void rejectsNonNumericExternalVacancyId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HabrCareerVacancyDetailsExtractor.buildVacancyUrl(
                        "1000167360?type=all"
                )
        );
    }

    @Test
    void recognisesExpectedVacancyDetailsUrl() {
        assertTrue(HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                "https://career.habr.com/vacancies/1000167360?source=list",
                "1000167360"
        ));
        assertFalse(HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                "https://career.habr.com/vacancies/1000167361",
                "1000167360"
        ));
    }

    @Test
    void findsJobPostingInsideJsonLdGraph() {
        Optional<JsonNode> result = HabrCareerVacancyDetailsExtractor
                .findJobPostingNode(
                        """
                                {
                                  "@graph": [
                                    {"@type": "Organization", "name": "Company"},
                                    {
                                      "@type": ["Thing", "JobPosting"],
                                      "identifier": {"value": "1000167360"}
                                    }
                                  ]
                                }
                                """,
                        objectMapper
                );

        assertTrue(result.isPresent());
        assertEquals("1000167360", result.get().at("/identifier/value").asText());
    }

    @Test
    void resolvesDetailsReadyOnlyForExpectedPageWithVacancyRoot() {
        assertEquals(
                HabrCareerVacancyDetailsProbeResultDto.Status
                        .VACANCY_DETAILS_READY,
                HabrCareerVacancyDetailsProbeService.resolveStatus(
                        "1000167360",
                        "https://career.habr.com/vacancies/1000167360",
                        true
                )
        );
    }

    @Test
    void resolvesAuthenticationRequiredForLoginRedirect() {
        assertEquals(
                HabrCareerVacancyDetailsProbeResultDto.Status
                        .AUTHENTICATION_REQUIRED,
                HabrCareerVacancyDetailsProbeService.resolveStatus(
                        "1000167360",
                        "https://account.habr.com/login",
                        false
                )
        );
    }
}
