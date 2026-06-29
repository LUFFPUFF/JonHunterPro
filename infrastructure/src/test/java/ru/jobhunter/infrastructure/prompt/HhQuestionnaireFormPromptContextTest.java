package ru.jobhunter.infrastructure.prompt;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HhQuestionnaireFieldType;
import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HhQuestionnaireFormPromptContextTest {

    @Test
    void shouldExposeQuestionnaireFieldsAsPlainTemplateMaps() {
        Map<String, Object> model = getStringObjectMap();

        List<?> questions = assertInstanceOf(
                List.class,
                model.get("questions")
        );

        assertEquals(2, questions.size());

        Map<?, ?> firstQuestion = assertInstanceOf(
                Map.class,
                questions.get(0)
        );

        Map<?, ?> secondQuestion = assertInstanceOf(
                Map.class,
                questions.get(1)
        );

        assertEquals(1, firstQuestion.get("questionIndex"));
        assertEquals(
                "TEXT",
                firstQuestion.get("fieldType")
        );
        assertEquals(
                "Готовы ли Вы работать в офисе "
                        + "с 9:00 до 18:00 "
                        + "в Санкт-Петербурге?",
                firstQuestion.get("questionText")
        );
        assertFalse(firstQuestion.containsKey("fieldName"));

        assertEquals(2, secondQuestion.get("questionIndex"));
        assertEquals("RADIO", secondQuestion.get("fieldType"));

        List<?> options = assertInstanceOf(
                List.class,
                secondQuestion.get("options")
        );

        Map<?, ?> firstOption = assertInstanceOf(
                Map.class,
                options.get(0)
        );

        assertEquals(1, firstOption.get("optionIndex"));
        assertEquals("Да", firstOption.get("label"));
    }

    private static @NonNull Map<String, Object> getStringObjectMap() {
        HhQuestionnaireFormPromptContext context =
                new HhQuestionnaireFormPromptContext(
                        "Переезд: не готов.",
                        "Нет",
                        "Java developer. Spring Boot. PostgreSQL.",
                        "Java developer",
                        "Example Company",
                        "Разработка backend-сервисов.",
                        List.of(
                                new HhQuestionnaireQuestionDto(
                                        "task_1_text",
                                        "Готовы ли Вы работать в офисе "
                                                + "с 9:00 до 18:00 "
                                                + "в Санкт-Петербурге?"
                                ),
                                new HhQuestionnaireQuestionDto(
                                        "task_2",
                                        "Рассматриваете ли Вы "
                                                + "тестовое задание?",
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
                                )
                        )
                );

        return context.toTemplateModel();
    }
}