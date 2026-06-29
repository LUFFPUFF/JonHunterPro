package ru.jobhunter.core.application.usecase.resume;

import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GetPrimaryResumeUseCase {

    CompletableFuture<Optional<ResumeDto>> getPrimaryResume(UserId userId);
}