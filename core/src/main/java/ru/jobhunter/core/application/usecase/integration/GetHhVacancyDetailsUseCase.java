package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HhVacancyDetailsDto;

import java.util.concurrent.CompletableFuture;

public interface GetHhVacancyDetailsUseCase {

    CompletableFuture<HhVacancyDetailsDto> getDetails(
            String externalVacancyId
    );
}