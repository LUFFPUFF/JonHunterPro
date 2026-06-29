package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.application.dto.UploadPrimaryResumePdfCommand;
import ru.jobhunter.core.application.port.out.document.PdfTextExtractionPort;
import ru.jobhunter.core.application.usecase.resume.SavePrimaryResumeUseCase;
import ru.jobhunter.core.domain.model.ResumeSourceType;
import ru.jobhunter.core.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfResumeUploadServiceTest {

    @Test
    void shouldExtractPdfTextAndSavePrimaryResume() {
        AtomicReference<String> capturedTitle = new AtomicReference<>();
        AtomicReference<String> capturedContent = new AtomicReference<>();
        AtomicReference<ResumeSourceType> capturedSourceType = new AtomicReference<>();

        PdfTextExtractionPort extractionPort =
                pdfBytes -> "Java, Spring Boot, PostgreSQL";

        SavePrimaryResumeUseCase savePrimaryResumeUseCase =
                (userId, title, sourceType, originalFileName, content) -> {
                    capturedTitle.set(title);
                    capturedContent.set(content);
                    capturedSourceType.set(sourceType);

                    return java.util.concurrent.CompletableFuture.completedFuture(
                            new ResumeDto(
                                    UUID.randomUUID(),
                                    userId.value(),
                                    title,
                                    sourceType,
                                    originalFileName,
                                    true,
                                    Instant.now(),
                                    Instant.now()
                            )
                    );
                };

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            PdfResumeUploadService service = new PdfResumeUploadService(
                    extractionPort,
                    savePrimaryResumeUseCase,
                    executorService
            );

            UserId userId = UserId.of(UUID.randomUUID());

            ResumeDto result = service.uploadPdf(
                    new UploadPrimaryResumePdfCommand(
                            userId,
                            "Java Backend Resume.pdf",
                            new byte[]{1, 2, 3}
                    )
            ).join();

            assertEquals("Java Backend Resume", capturedTitle.get());
            assertEquals(
                    "Java, Spring Boot, PostgreSQL",
                    capturedContent.get()
            );
            assertEquals(
                    ResumeSourceType.UPLOADED_PDF,
                    capturedSourceType.get()
            );

            assertEquals(userId.value(), result.userId());
            assertTrue(result.primary());
        }
    }
}