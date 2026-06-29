package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionnaireResumeEvidenceCandidateExtractorTest {

    private final QuestionnaireResumeEvidenceCandidateExtractor extractor =
            new QuestionnaireResumeEvidenceCandidateExtractor();

    @Test
    void shouldFindDirectEvidenceForJavaMentionedInResume() {
        Optional<DirectResumeEvidenceCandidate> result =
                extractor.findDirectEvidence(
                        "Как оцениваете свой уровень владения Java?",
                        """
                        Разрабатывал и поддерживал Java-сервисы
                        на Spring Boot и PostgreSQL.
                        """
                );

        assertTrue(result.isPresent());

        DirectResumeEvidenceCandidate evidence = result.orElseThrow();

        assertEquals("Java", evidence.topic());
        assertTrue(evidence.quote().contains("Java-сервисы"));
        assertTrue(evidence.matchedTerms().contains("java"));
    }

    @Test
    void shouldNotCreateDirectEvidenceWhenNamedSkillIsAbsent() {
        Optional<DirectResumeEvidenceCandidate> result =
                extractor.findDirectEvidence(
                        "Был ли у вас опыт работы с Selenium, Selenide?",
                        """
                        Разрабатывал Java-сервисы на Spring Boot.
                        Работал с PostgreSQL и Kafka.
                        """
                );

        assertFalse(result.isPresent());
    }

    @Test
    void shouldNotCreateDirectEvidenceForGenericQuestionWithoutNamedTerm() {
        Optional<DirectResumeEvidenceCandidate> result =
                extractor.findDirectEvidence(
                        "Есть ли опыт работы с базами данных?",
                        """
                        Разрабатывал Java-сервисы на Spring Boot.
                        Работал с PostgreSQL и Kafka.
                        """
                );

        assertFalse(result.isPresent());
    }
}