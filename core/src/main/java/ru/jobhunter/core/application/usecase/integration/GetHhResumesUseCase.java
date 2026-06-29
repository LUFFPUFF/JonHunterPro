package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HhResumeDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetHhResumesUseCase {

    CompletableFuture<List<HhResumeDto>> getResumes(UserId userId);
}