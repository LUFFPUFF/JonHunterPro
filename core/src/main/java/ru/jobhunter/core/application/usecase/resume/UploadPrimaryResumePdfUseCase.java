package ru.jobhunter.core.application.usecase.resume;

import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.application.dto.UploadPrimaryResumePdfCommand;

import java.util.concurrent.CompletableFuture;

public interface UploadPrimaryResumePdfUseCase {

    CompletableFuture<ResumeDto> uploadPdf(UploadPrimaryResumePdfCommand command);
}