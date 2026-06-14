package ru.jobhunter.infrastructure.platform.hh.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhCurrentUserResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancySearchRequest;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancySearchResponse;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class HhApiClient {

    private static final Logger log = LoggerFactory.getLogger(HhApiClient.class);

    private static final String ME_PATH = "/me";
    private static final String VACANCIES_PATH = "/vacancies";

    private final HhApiRequestExecutor requestExecutor;

    public HhApiClient(HhApiRequestExecutor requestExecutor) {
        this.requestExecutor = Objects.requireNonNull(
                requestExecutor,
                "HH API request executor must not be null"
        );
    }

    public CompletableFuture<HhCurrentUserResponse> getCurrentUser(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        log.info("Requesting current HH.ru user information");

        return requestExecutor.getAuthorized(
                ME_PATH,
                Map.of(),
                accessToken,
                HhCurrentUserResponse.class
        );
    }

    public CompletableFuture<HhVacancySearchResponse> searchVacancies(
            HhVacancySearchRequest request
    ) {
        Objects.requireNonNull(request, "HH vacancy search request must not be null");

        log.info(
                "Searching HH.ru vacancies: textProvided={}, areaProvided={}, page={}, perPage={}",
                request.text() != null && !request.text().isBlank(),
                request.area() != null && !request.area().isBlank(),
                request.page(),
                request.perPage()
        );

        return requestExecutor.getPublic(
                VACANCIES_PATH,
                request.toQueryParameters(),
                HhVacancySearchResponse.class
        );
    }
}
