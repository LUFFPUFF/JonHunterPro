package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerBrowserSessionProbeResultDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.concurrent.CompletableFuture;

public interface ProbeHabrCareerBrowserSessionUseCase {

    CompletableFuture<HabrCareerBrowserSessionProbeResultDto> probe(UserId userId);
}
