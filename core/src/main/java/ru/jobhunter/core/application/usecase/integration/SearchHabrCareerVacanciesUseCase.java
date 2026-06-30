package ru.jobhunter.core.application.usecase.integration;

import ru.jobhunter.core.application.dto.HabrCareerVacancyFullSearchResultDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchProgressDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchQuery;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchResultDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface SearchHabrCareerVacanciesUseCase {

    CompletableFuture<HabrCareerVacancySearchResultDto> search(
            UserId userId,
            HabrCareerVacancySearchQuery query
    );

    CompletableFuture<HabrCareerVacancyFullSearchResultDto> searchAll(
            UserId userId,
            HabrCareerVacancySearchQuery query,
            Consumer<HabrCareerVacancySearchProgressDto> progressConsumer
    );

    default CompletableFuture<HabrCareerVacancyFullSearchResultDto> searchAll(
            UserId userId,
            HabrCareerVacancySearchQuery query
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(query, "Habr Career search query must not be null");

        return searchAll(userId, query, ignored -> {
        });
    }
}
