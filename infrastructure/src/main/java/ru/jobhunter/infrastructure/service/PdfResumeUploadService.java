package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.application.dto.UploadPrimaryResumePdfCommand;
import ru.jobhunter.core.application.port.out.document.PdfTextExtractionPort;
import ru.jobhunter.core.application.usecase.resume.SavePrimaryResumeUseCase;
import ru.jobhunter.core.application.usecase.resume.UploadPrimaryResumePdfUseCase;
import ru.jobhunter.core.domain.model.ResumeSourceType;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public final class PdfResumeUploadService implements UploadPrimaryResumePdfUseCase {

    private static final Logger log = LoggerFactory.getLogger(PdfResumeUploadService.class);

    private final PdfTextExtractionPort pdfTextExtractionPort;
    private final SavePrimaryResumeUseCase savePrimaryResumeUseCase;
    private final ExecutorService executorService;

    public PdfResumeUploadService(
            PdfTextExtractionPort pdfTextExtractionPort,
            SavePrimaryResumeUseCase savePrimaryResumeUseCase,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.pdfTextExtractionPort = Objects.requireNonNull(
                pdfTextExtractionPort,
                "PDF text extraction port must not be null"
        );
        this.savePrimaryResumeUseCase = Objects.requireNonNull(
                savePrimaryResumeUseCase,
                "Save primary resume use case must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Executor service must not be null"
        );
    }

    @Override
    public CompletableFuture<ResumeDto> uploadPdf(
            UploadPrimaryResumePdfCommand command
    ) {
        Objects.requireNonNull(command, "Upload resume PDF command must not be null");

        log.info(
                "Uploading primary resume PDF: userId={}, fileName={}, fileSizeBytes={}",
                command.userId(),
                command.originalFileName(),
                command.pdfBytes().length
        );

        return CompletableFuture
                .supplyAsync(
                        () -> pdfTextExtractionPort.extractText(command.pdfBytes()),
                        executorService
                )
                .thenCompose(extractedText ->
                        savePrimaryResumeUseCase.savePrimaryResume(
                                command.userId(),
                                titleFromFileName(command.originalFileName()),
                                ResumeSourceType.UPLOADED_PDF,
                                command.originalFileName(),
                                extractedText
                        )
                );
    }

    private String titleFromFileName(String fileName) {
        int extensionStart = fileName.lastIndexOf('.');

        if (extensionStart <= 0) {
            return fileName;
        }

        return fileName.substring(0, extensionStart).trim();
    }
}