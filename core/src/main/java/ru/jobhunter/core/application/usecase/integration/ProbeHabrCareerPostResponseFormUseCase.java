package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerPostResponseFormProbeResultDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface ProbeHabrCareerPostResponseFormUseCase {

    CompletableFuture<HabrCareerPostResponseFormProbeResultDto> probe(
            UserId userId,
            String externalVacancyId
    );
}
