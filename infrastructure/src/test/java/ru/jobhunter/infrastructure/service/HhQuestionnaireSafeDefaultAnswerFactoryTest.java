package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswerDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireAnswerQuality;
import ru.jobhunter.core.application.dto.HhQuestionnaireFieldType;
import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HhQuestionnaireSafeDefaultAnswerFactoryTest {

    @Test
    void shouldUseOtherOptionForUnknownDockerExperience() {
        HhQuestionnaireQuestionDto question =
                new HhQuestionnaireQuestionDto(
                        "task_docker",
                        "Опыт работы с Docker и Kubernetes?",
                        HhQuestionnaireFieldType.RADIO_WITH_OTHER_TEXT,
                        List.of(
                                new HhQuestionnaireOptionDto(
                                        "yes",
                                        "Да"
                                ),
                                new HhQuestionnaireOptionDto(
                                        "no",
                                        "Нет"
                                ),
                                new HhQuestionnaireOptionDto(
                                        "open",
                                        "Свой вариант"
                                )
                        ),
                        "open",
                        "task_docker_text"
                );

        GeneratedHhQuestionnaireAnswerDto result =
                HhQuestionnaireSafeDefaultAnswerFactory
                        .tryCreate(1, question)
                        .orElseThrow();

        assertEquals(
                HhQuestionnaireAnswerQuality.SAFE_DEFAULT,
                result.quality()
        );
        assertEquals("open", result.selectedOptionValue());
        assertFalse(result.answer().isBlank());
    }

    @Test
    void shouldCreateNeutralTextForEducationQuestion() {
        HhQuestionnaireQuestionDto question =
                new HhQuestionnaireQuestionDto(
                        "task_education",
                        "Наличие высшего образования?"
                );

        GeneratedHhQuestionnaireAnswerDto result =
                HhQuestionnaireSafeDefaultAnswerFactory
                        .tryCreate(1, question)
                        .orElseThrow();

        assertEquals(
                HhQuestionnaireAnswerQuality.SAFE_DEFAULT,
                result.quality()
        );
        assertTrue(
                result.answer().contains("образовании")
        );
    }

    @Test
    void shouldNotCreateFallbackForPlainRadioQuestion() {
        HhQuestionnaireQuestionDto question =
                new HhQuestionnaireQuestionDto(
                        "task_binary",
                        "Есть ли коммерческий опыт Go?",
                        HhQuestionnaireFieldType.RADIO,
                        List.of(
                                new HhQuestionnaireOptionDto(
                                        "yes",
                                        "Да"
                                ),
                                new HhQuestionnaireOptionDto(
                                        "no",
                                        "Нет"
                                )
                        )
                );

        assertTrue(
                HhQuestionnaireSafeDefaultAnswerFactory
                        .tryCreate(1, question)
                        .isEmpty()
        );
    }
}