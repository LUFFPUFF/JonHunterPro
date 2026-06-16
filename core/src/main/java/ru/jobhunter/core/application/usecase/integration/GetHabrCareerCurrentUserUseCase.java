package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerCurrentUserDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface GetHabrCareerCurrentUserUseCase {

    CompletableFuture<HabrCareerCurrentUserDto> getCurrentUser(UserId userId);
}
