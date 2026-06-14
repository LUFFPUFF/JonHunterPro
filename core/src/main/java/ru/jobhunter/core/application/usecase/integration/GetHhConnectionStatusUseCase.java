package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HhConnectionStatusDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface GetHhConnectionStatusUseCase {

    CompletableFuture<HhConnectionStatusDto> getStatus(UserId userId);
}
