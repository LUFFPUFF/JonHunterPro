package ru.jobhunter.infrastructure.platform.habr.browser;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerResponseFormProbeResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabrCareerResponseFormProbeServiceTest {

    @Test
    void resolvesDirectResponseAsUnsafeWithoutAnyClick() {
        assertEquals(
                HabrCareerResponseFormProbeResultDto.Status
                        .DIRECT_RESPONSE_WOULD_SEND_IMMEDIATELY,
                HabrCareerResponseFormProbeService.resolveStatus(
                        "1000167360",
                        "https://career.habr.com/vacancies/1000167360",
                        true,
                        true,
                        true
                )
        );
    }

    @Test
    void resolvesUnknownResponseActionAsNotClickedForSafety() {
        assertEquals(
                HabrCareerResponseFormProbeResultDto.Status
                        .RESPONSE_ACTION_PRESENT_NOT_CLICKED_FOR_SAFETY,
                HabrCareerResponseFormProbeService.resolveStatus(
                        "1000167360",
                        "https://career.habr.com/vacancies/1000167360",
                        true,
                        true,
                        false
                )
        );
    }

    @Test
    void resolvesAuthenticationRequiredForLoginRedirect() {
        assertEquals(
                HabrCareerResponseFormProbeResultDto.Status
                        .AUTHENTICATION_REQUIRED,
                HabrCareerResponseFormProbeService.resolveStatus(
                        "1000167360",
                        "https://account.habr.com/login",
                        false,
                        false,
                        false
                )
        );
    }

    @Test
    void recognisesOnlyInitialResponseButtonText() {
        assertTrue(HabrCareerResponseFormExtractor
                .isInitialResponseActionText(" Откликнуться "));
        assertFalse(HabrCareerResponseFormExtractor
                .isInitialResponseActionText("Отправить отклик"));
    }

    @Test
    void recognisesDirectResponseKind() {
        assertTrue(HabrCareerResponseFormExtractor
                .isDirectResponseKind("direct"));
        assertFalse(HabrCareerResponseFormExtractor
                .isDirectResponseKind("questionnaire"));
    }

    @Test
    void recognisesKnownTerminalResponseMarkers() {
        assertTrue(HabrCareerResponseFormExtractor
                .containsTerminalResponseMarker("Ваш отклик отправлен"));
        assertFalse(HabrCareerResponseFormExtractor
                .containsTerminalResponseMarker("Ваш отклик"));
    }
}
