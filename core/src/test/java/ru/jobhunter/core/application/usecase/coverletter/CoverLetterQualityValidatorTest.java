package ru.jobhunter.core.application.usecase.coverletter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoverLetterQualityValidatorTest {

    @Test
    void shouldAcceptDetailedCoverLetter() {
        String coverLetter = """
                Здравствуйте! Меня заинтересовала вакансия Java-разработчика,
                потому что в ней важны разработка серверных сервисов,
                интеграции и работа с данными.

                В учебных и практических проектах я использовал Java,
                Spring Boot, PostgreSQL, Docker и Kafka. Также я работал
                с проектированием API, обработкой ошибок и подготовкой
                технической документации. Буду рад подробнее обсудить,
                каким образом мой опыт может быть полезен вашей команде.
                """;

        String result = CoverLetterQualityValidator
                .validateAndNormalize(coverLetter);

        assertEquals(coverLetter.strip(), result);
    }

    @Test
    void shouldRejectTooShortCoverLetter() {
        GeneratedCoverLetterQualityException exception = assertThrows(
                GeneratedCoverLetterQualityException.class,
                () -> CoverLetterQualityValidator.validateAndNormalize(
                        "Здравствуйте, мне интересна вакансия."
                )
        );

        assertTrue(exception.getMessage().contains("length="));
    }

    @Test
    void shouldRejectLongTextWithOneSentence() {
        String singleSentence = "Опыт Java и Spring Boot ".repeat(30) + ".";

        GeneratedCoverLetterQualityException exception = assertThrows(
                GeneratedCoverLetterQualityException.class,
                () -> CoverLetterQualityValidator.validateAndNormalize(
                        singleSentence
                )
        );

        assertTrue(exception.getMessage().contains("sentences="));
    }
}