package ru.jobhunter.infrastructure.platform.habr.browser;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HabrCareerPostResponseFormProbeResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HabrCareerPostResponseFormProbeServiceTest {

    private static final String VACANCY_ID = "1000167360";
    private static final String VACANCY_URL =
            "https://career.habr.com/vacancies/1000167360";

    @Test
    void resolvesReadyWhenTerminalMarkerAndComplementFormArePresent() {
        assertEquals(
                HabrCareerPostResponseFormProbeResultDto.Status
                        .POST_RESPONSE_FORM_READY,
                HabrCareerPostResponseFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        "Отклик отправлен",
                        true,
                        false,
                        false
                )
        );
    }

    @Test
    void resolvesExistingEditableResponseBeforeNoResponseState() {
        assertEquals(
                HabrCareerPostResponseFormProbeResultDto.Status
                        .RESPONSE_EXISTS_EDITABLE,
                HabrCareerPostResponseFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        "",
                        false,
                        true,
                        false
                )
        );
    }

    @Test
    void resolvesTerminalStateWithoutFormSeparately() {
        assertEquals(
                HabrCareerPostResponseFormProbeResultDto.Status
                        .TERMINAL_RESPONSE_STATE_WITHOUT_COMPLEMENT_FORM,
                HabrCareerPostResponseFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        "Отклик отправлен",
                        false,
                        false,
                        false
                )
        );
    }

    @Test
    void resolvesInitialActionWhenResponseHasNotBeenSent() {
        assertEquals(
                HabrCareerPostResponseFormProbeResultDto.Status
                        .INITIAL_RESPONSE_ACTION_STILL_AVAILABLE,
                HabrCareerPostResponseFormProbeService.resolveStatus(
                        VACANCY_ID,
                        VACANCY_URL,
                        true,
                        "",
                        false,
                        false,
                        true
                )
        );
    }

    @Test
    void resolvesAuthenticationRequiredForLoginRedirect() {
        assertEquals(
                HabrCareerPostResponseFormProbeResultDto.Status
                        .AUTHENTICATION_REQUIRED,
                HabrCareerPostResponseFormProbeService.resolveStatus(
                        VACANCY_ID,
                        "https://account.habr.com/login",
                        false,
                        "",
                        false,
                        false,
                        false
                )
        );
    }

    @Test
    void recognisesExactEditAndComplementActionText() {
        assertTrue(HabrCareerResponseFormExtractor
                .isExistingResponseEditActionText(" Редактировать "));
        assertTrue(HabrCareerResponseFormExtractor
                .isComplementResponseActionText(" Дополнить отклик "));
        assertFalse(HabrCareerResponseFormExtractor
                .isExistingResponseEditActionText("Удалить"));
    }
}
