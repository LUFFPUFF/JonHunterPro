package ru.jobhunter.infrastructure.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClasspathJinjaPromptTemplateRendererTest {

    private final PromptTemplateRenderer renderer =
            new ClasspathJinjaPromptTemplateRenderer();

    @Test
    void shouldRenderCoverLetterPromptFromClasspathTemplate() {
        String rendered = renderer.render(
                PromptTemplate.COVER_LETTER_USER,
                new CoverLetterPromptContext(
                        "HH_RU",
                        "123",
                        "Java developer",
                        "Example Company",
                        "https://hh.ru/vacancy/123",
                        "Разработка Java-сервисов.",
                        "Опыт Java и Spring Boot."
                )
        );

        assertThat(rendered)
                .contains("Java developer")
                .contains("Example Company")
                .contains("Опыт Java и Spring Boot.")
                .doesNotContain("{{ vacancy_title }}");
    }

    @Test
    void shouldRenderQuestionnairePromptFromClasspathTemplate() {
        String rendered = renderer.render(
                PromptTemplate.HH_QUESTIONNAIRE_SINGLE_ANSWER_USER,
                new HhSingleQuestionPromptContext(
                        "Часовой пояс: Europe/Moscow.",
                        "Подтверждённый факт кандидата.",
                        "Java, Spring Boot, PostgreSQL.",
                        "task_123_text",
                        "Есть ли опыт работы с Java?"
                )
        );

        assertThat(rendered)
                .contains("Часовой пояс: Europe/Moscow.")
                .contains("task_123_text")
                .contains("Есть ли опыт работы с Java?")
                .contains("Java, Spring Boot, PostgreSQL.")
                .doesNotContain("{{ question_text }}");
    }
}