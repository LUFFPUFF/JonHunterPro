package ru.jobhunter.infrastructure.platform.habr.browser;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerResponseEditFormProbeResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HabrCareerResponseEditFormProbeServiceTest {

    private static final String VACANCY_ID = "1000167360";
    private static final String VACANCY_URL =
            "https://career.habr.com/vacancies/1000167360";

    @Test
    void resolvesReadyOnlyAfterExactEditActionAndControlsArePresent() {
        assertEquals(
                HabrCareerResponseEditFormProbeResultDto.Status.EDIT_FORM_READY,
                HabrCareerResponseEditFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        true,
                        true,
                        true
                )
        );
    }

    @Test
    void resolvesNotRenderedWhenEditWasClickedButControlsDidNotAppear() {
        assertEquals(
                HabrCareerResponseEditFormProbeResultDto.Status
                        .RESPONSE_EXISTS_EDIT_FORM_NOT_RENDERED,
                HabrCareerResponseEditFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        true,
                        true,
                        false
                )
        );
    }

    @Test
    void resolvesNotFoundWhenExistingResponseIsAbsent() {
        assertEquals(
                HabrCareerResponseEditFormProbeResultDto.Status.RESPONSE_NOT_FOUND,
                HabrCareerResponseEditFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        false,
                        false,
                        false
                )
        );
    }

    @Test
    void resolvesAuthenticationRequiredForLoginRedirect() {
        assertEquals(
                HabrCareerResponseEditFormProbeResultDto.Status
                        .AUTHENTICATION_REQUIRED,
                HabrCareerResponseEditFormProbeService.resolveStatus(
                        VACANCY_ID,
                        "https://account.habr.com/login",
                        false,
                        false,
                        false,
                        false
                )
        );
    }
}
