package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HhCurrentUserDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface GetHhCurrentUserUseCase {

    CompletableFuture<HhCurrentUserDto> getCurrentUser(UserId userId);
}
