package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerResponseEditFormProbeResultDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface ProbeHabrCareerResponseEditFormUseCase {

    CompletableFuture<HabrCareerResponseEditFormProbeResultDto> probe(
            UserId userId,
            String externalVacancyId
    );
}
