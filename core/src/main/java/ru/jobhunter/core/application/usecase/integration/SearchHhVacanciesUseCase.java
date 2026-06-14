package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HhVacancySearchQuery;
import ru.jobhunter.core.application.dto.HhVacancySearchResultDto;

import java.util.concurrent.CompletableFuture;

public interface SearchHhVacanciesUseCase {

    CompletableFuture<HhVacancySearchResultDto> search(HhVacancySearchQuery query);
}
