package ru.jobhunter.core.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeTest {

    @Test
    void shouldCreatePrimaryResume() {
        UserId userId = UserId.of(
                UUID.fromString("7b908e12-fe65-4aa2-b78c-a9c002ad6d2e")
        );

        Resume resume = Resume.createPrimary(
                userId,
                "Java Backend Resume",
                ResumeSourceType.UPLOADED_PDF,
                "resume.pdf",
                "Java, Spring Boot, PostgreSQL, JavaFX"
        );

        assertEquals(userId, resume.userId());
        assertEquals("Java Backend Resume", resume.title());
        assertEquals(ResumeSourceType.UPLOADED_PDF, resume.sourceType());
        assertEquals("resume.pdf", resume.originalFileName());
        assertTrue(resume.primary());
        assertTrue(resume.content().contains("Spring Boot"));
    }

    @Test
    void shouldConvertResumeToNonPrimary() {
        Resume resume = Resume.createPrimary(
                UserId.of(UUID.randomUUID()),
                "Resume",
                ResumeSourceType.UPLOADED_PDF,
                "resume.pdf",
                "Resume content"
        );

        Resume nonPrimaryResume = resume.asNonPrimary();

        assertFalse(nonPrimaryResume.primary());
        assertEquals(resume.id(), nonPrimaryResume.id());
        assertEquals(resume.userId(), nonPrimaryResume.userId());
    }

    @Test
    void shouldRejectBlankResumeContent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Resume.createPrimary(
                        UserId.of(UUID.randomUUID()),
                        "Resume",
                        ResumeSourceType.UPLOADED_PDF,
                        "resume.pdf",
                        "   "
                )
        );
    }
}