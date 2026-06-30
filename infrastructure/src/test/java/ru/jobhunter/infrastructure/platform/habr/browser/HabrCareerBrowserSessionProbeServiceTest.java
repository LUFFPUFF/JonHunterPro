package ru.jobhunter.infrastructure.platform.habr.browser;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerBrowserSessionProbeResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HabrCareerBrowserSessionProbeServiceTest {

    @Test
    void resolvesAuthenticatedWhenProfilePageStaysOnCareerHabrCom() {
        HabrCareerBrowserSessionProbeResultDto.Status status =
                HabrCareerBrowserSessionProbeService.resolveStatus(
                        "https://career.habr.com/profile"
                );

        assertEquals(
                HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATED,
                status
        );
    }

    @Test
    void resolvesAuthenticatedWhenProfileRedirectsToCareerHabrComUserPage() {
        HabrCareerBrowserSessionProbeResultDto.Status status =
                HabrCareerBrowserSessionProbeService.resolveStatus(
                        "https://career.habr.com/users/candidate-login"
                );

        assertEquals(
                HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATED,
                status
        );
    }

    @Test
    void resolvesAuthenticationRequiredForHabrLoginPage() {
        HabrCareerBrowserSessionProbeResultDto.Status status =
                HabrCareerBrowserSessionProbeService.resolveStatus(
                        "https://career.habr.com/auth/login?next=%2Fprofile"
                );

        assertEquals(
                HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATION_REQUIRED,
                status
        );
    }

    @Test
    void resolvesUnexpectedPageForExternalRedirect() {
        HabrCareerBrowserSessionProbeResultDto.Status status =
                HabrCareerBrowserSessionProbeService.resolveStatus(
                        "https://example.org/login"
                );

        assertEquals(
                HabrCareerBrowserSessionProbeResultDto.Status.UNEXPECTED_PAGE,
                status
        );
    }
}
