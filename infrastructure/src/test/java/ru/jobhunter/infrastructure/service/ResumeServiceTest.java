package ru.jobhunter.infrastructure.service;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.PrimaryResumeContentDto;
import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.domain.model.Resume;
import ru.jobhunter.core.domain.model.ResumeSourceType;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ResumeRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeServiceTest {

    @Test
    void shouldSaveAndReturnPrimaryResume() {
        ResumeService service = getResumeService();

        UserId userId = UserId.of(
                UUID.fromString("7b908e12-fe65-4aa2-b78c-a9c002ad6d2e")
        );

        ResumeDto savedResume = service.savePrimaryResume(
                userId,
                "Java Backend Resume",
                ResumeSourceType.UPLOADED_PDF,
                "java-backend-resume.pdf",
                "Java, Spring Boot, PostgreSQL, JavaFX"
        ).join();

        assertEquals(userId.value(), savedResume.userId());
        assertEquals("Java Backend Resume", savedResume.title());
        assertEquals(ResumeSourceType.UPLOADED_PDF, savedResume.sourceType());
        assertEquals("java-backend-resume.pdf", savedResume.originalFileName());
        assertTrue(savedResume.primary());

        Optional<PrimaryResumeContentDto> content = service
                .getPrimaryResumeContent(userId)
                .join();

        assertTrue(content.isPresent());
        assertEquals("Java Backend Resume", content.get().title());
        assertTrue(content.get().content().contains("Spring Boot"));
    }

    private static @NonNull ResumeService getResumeService() {
        AtomicReference<Resume> storedResume = new AtomicReference<>();

        ResumeRepository repository = new ResumeRepository() {
            @Override
            public CompletableFuture<Resume> replacePrimaryResume(Resume resume) {
                storedResume.set(resume);
                return CompletableFuture.completedFuture(resume);
            }

            @Override
            public CompletableFuture<Optional<Resume>> findPrimaryByUserId(UserId userId) {
                Resume resume = storedResume.get();

                if (resume == null || !resume.userId().equals(userId)) {
                    return CompletableFuture.completedFuture(Optional.empty());
                }

                return CompletableFuture.completedFuture(Optional.of(resume));
            }
        };

        return new ResumeService(repository);
    }
}