package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsProbeResultDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface ProbeHabrCareerVacancyDetailsUseCase {

    CompletableFuture<HabrCareerVacancyDetailsProbeResultDto> probe(
            UserId userId,
            String externalVacancyId
    );
}
