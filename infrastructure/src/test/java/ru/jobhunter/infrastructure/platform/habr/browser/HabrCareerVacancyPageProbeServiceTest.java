package ru.jobhunter.infrastructure.platform.habr.browser;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerVacancyPageProbeResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HabrCareerVacancyPageProbeServiceTest {

    @Test
    void resolvesReadyStatusForVacanciesPageWithCards() {
        HabrCareerVacancyPageProbeResultDto.Status status =
                HabrCareerVacancyPageProbeService.resolveStatus(
                        "https://career.habr.com/vacancies?page=2",
                        25
                );

        assertEquals(
                HabrCareerVacancyPageProbeResultDto.Status.VACANCY_LIST_READY,
                status
        );
    }

    @Test
    void resolvesAuthenticationRequiredForLoginPage() {
        HabrCareerVacancyPageProbeResultDto.Status status =
                HabrCareerVacancyPageProbeService.resolveStatus(
                        "https://account.habr.com/login",
                        0
                );

        assertEquals(
                HabrCareerVacancyPageProbeResultDto.Status.AUTHENTICATION_REQUIRED,
                status
        );
    }

    @Test
    void resolvesCardsNotFoundForEmptyVacanciesPage() {
        HabrCareerVacancyPageProbeResultDto.Status status =
                HabrCareerVacancyPageProbeService.resolveStatus(
                        "https://career.habr.com/vacancies",
                        0
                );

        assertEquals(
                HabrCareerVacancyPageProbeResultDto.Status.VACANCY_CARDS_NOT_FOUND,
                status
        );
    }
}
