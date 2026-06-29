package ru.jobhunter.infrastructure.platform.hh.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HhSalaryDto;
import ru.jobhunter.core.application.dto.HhVacancyDto;
import ru.jobhunter.core.application.dto.HhVacancySearchQuery;
import ru.jobhunter.core.application.dto.HhVacancySearchResultDto;
import ru.jobhunter.core.application.usecase.integration.SearchHhVacanciesUseCase;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhDictionaryItemResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhEmployerShortResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhSalaryResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancyItemResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancySearchRequest;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancySearchResponse;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class HhVacancySearchService implements SearchHhVacanciesUseCase {

    private static final Logger log = LoggerFactory.getLogger(HhVacancySearchService.class);

    private final HhApiClient apiClient;
    private final HhApplicationTokenProperties applicationTokenProperties;

    public HhVacancySearchService(
            HhApiClient apiClient,
            HhApplicationTokenProperties applicationTokenProperties
    ) {
        this.apiClient = Objects.requireNonNull(
                apiClient,
                "HH API client must not be null"
        );
        this.applicationTokenProperties = Objects.requireNonNull(
                applicationTokenProperties,
                "HH application token properties must not be null"
        );
    }

    @Override
    public CompletableFuture<HhVacancySearchResultDto> search(HhVacancySearchQuery query) {
        Objects.requireNonNull(query, "HH vacancy search query must not be null");

        HhVacancySearchRequest request = new HhVacancySearchRequest(
                query.text(),
                query.area(),
                query.page(),
                query.perPage()
        );

        log.info(
                "Searching HH.ru vacancies through use case with application token: textProvided={}, areaProvided={}, page={}, perPage={}",
                query.text() != null && !query.text().isBlank(),
                query.area() != null && !query.area().isBlank(),
                query.page(),
                query.perPage()
        );

        String applicationAccessToken = applicationTokenProperties.requireAccessToken();

        return apiClient.searchVacanciesAuthorized(request, applicationAccessToken)
                .thenApply(this::toDto);
    }

    private HhVacancySearchResultDto toDto(HhVacancySearchResponse response) {
        List<HhVacancyDto> vacancies = response.items() == null
                ? List.of()
                : response.items().stream()
                .map(this::toVacancyDto)
                .toList();

        return new HhVacancySearchResultDto(
                vacancies,
                valueOrZero(response.found()),
                valueOrZero(response.pages()),
                valueOrZero(response.page()),
                valueOrZero(response.perPage())
        );
    }

    private HhVacancyDto toVacancyDto(HhVacancyItemResponse vacancy) {
        HhDictionaryItemResponse area = vacancy.area();
        HhEmployerShortResponse employer = vacancy.employer();
        HhDictionaryItemResponse experience = vacancy.experience();
        HhDictionaryItemResponse employment = vacancy.employment();
        HhDictionaryItemResponse schedule = vacancy.schedule();

        return new HhVacancyDto(
                vacancy.id(),
                vacancy.name(),
                vacancy.url(),
                vacancy.alternateUrl(),
                idOf(area),
                nameOf(area),
                employerIdOf(employer),
                employerNameOf(employer),
                employerUrlOf(employer),
                toSalaryDto(vacancy.salary()),
                idOf(experience),
                nameOf(experience),
                idOf(employment),
                nameOf(employment),
                idOf(schedule),
                nameOf(schedule)
        );
    }

    private HhSalaryDto toSalaryDto(HhSalaryResponse salary) {
        if (salary == null) {
            return null;
        }

        return new HhSalaryDto(
                salary.from(),
                salary.to(),
                salary.currency(),
                salary.gross()
        );
    }

    private String idOf(HhDictionaryItemResponse item) {
        return item == null ? null : item.id();
    }

    private String nameOf(HhDictionaryItemResponse item) {
        return item == null ? null : item.name();
    }

    private String employerIdOf(HhEmployerShortResponse employer) {
        return employer == null ? null : employer.id();
    }

    private String employerNameOf(HhEmployerShortResponse employer) {
        return employer == null ? null : employer.name();
    }

    private String employerUrlOf(HhEmployerShortResponse employer) {
        if (employer == null) {
            return null;
        }

        return employer.alternateUrl() != null
                ? employer.alternateUrl()
                : employer.url();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}