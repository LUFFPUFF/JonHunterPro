package ru.jobhunter.core.application.usecase.resume;

import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.domain.model.ResumeSourceType;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface SavePrimaryResumeUseCase {

    CompletableFuture<ResumeDto> savePrimaryResume(
            UserId userId,
            String title,
            ResumeSourceType sourceType,
            String originalFileName,
            String content
    );
}