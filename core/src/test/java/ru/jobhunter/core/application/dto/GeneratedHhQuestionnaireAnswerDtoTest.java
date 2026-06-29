package ru.jobhunter.core.application.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratedHhQuestionnaireAnswerDtoTest {

    @Test
    void shouldAcceptProfileDerivedTextAnswer() {
        assertDoesNotThrow(() ->
                new GeneratedHhQuestionnaireAnswerDto(
                        "task_1_text",
                        "Для такой задачи я бы начал с анализа "
                                + "требований и проверки позитивных "
                                + "и негативных сценариев.",
                        "",
                        HhQuestionnaireAnswerQuality.PROFILE_DERIVED,
                        "",
                        List.of(
                                "LLM_FORM:questionIndex=1",
                                "PROFILE_DERIVED"
                        )
                )
        );
    }

    @Test
    void shouldAcceptProfileDerivedRadioAnswer() {
        assertDoesNotThrow(() ->
                new GeneratedHhQuestionnaireAnswerDto(
                        "task_2",
                        "",
                        "option_2",
                        HhQuestionnaireAnswerQuality.PROFILE_DERIVED,
                        "",
                        List.of(
                                "LLM_FORM:questionIndex=2",
                                "PROFILE_DERIVED"
                        )
                )
        );
    }

    @Test
    void shouldRejectProfileDerivedAnswerWithoutEvidence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GeneratedHhQuestionnaireAnswerDto(
                        "task_1_text",
                        "Могу описать технический подход.",
                        "",
                        HhQuestionnaireAnswerQuality.PROFILE_DERIVED,
                        "",
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectReviewRequiredAnswerWithSelectedOption() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GeneratedHhQuestionnaireAnswerDto(
                        "task_2",
                        "",
                        "option_2",
                        HhQuestionnaireAnswerQuality.REVIEW_REQUIRED,
                        "Нужен подтверждённый факт кандидата.",
                        List.of(
                                "REVIEW:PROFILE_FACT_MISSING"
                        )
                )
        );
    }
}