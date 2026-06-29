package ru.jobhunter.core.application.usecase.resume;

import ru.jobhunter.core.application.dto.PrimaryResumeContentDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GetPrimaryResumeContentUseCase {

    CompletableFuture<Optional<PrimaryResumeContentDto>> getPrimaryResumeContent(
            UserId userId
    );
}