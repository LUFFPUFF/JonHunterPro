package ru.jobhunter.infrastructure.platform.hh.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.hh.api.dto.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class HhApiClient {

    private static final Logger log = LoggerFactory.getLogger(HhApiClient.class);

    private static final String ME_PATH = "/me";
    private static final String VACANCIES_PATH = "/vacancies";
    private static final String VACANCY_DETAILS_PATH_TEMPLATE = "/vacancies/%s";
    private static final String APPLY_TO_VACANCY_PATH_TEMPLATE = "/vacancies/%s/application";
    private static final String NEGOTIATIONS_PATH = "/negotiations";
    private static final String MY_RESUMES_PATH = "/resumes/mine";
    private static final String SUITABLE_RESUMES_PATH_TEMPLATE = "/vacancies/%s/suitable_resumes";

    private final HhApiRequestExecutor requestExecutor;

    public HhApiClient(HhApiRequestExecutor requestExecutor) {
        this.requestExecutor = Objects.requireNonNull(
                requestExecutor,
                "HH API request executor must not be null"
        );
    }

    public CompletableFuture<HhSuitableResumesResponse> getSuitableResumes(
            String vacancyId,
            String accessToken
    ) {
        String normalizedVacancyId = requireNotBlank(vacancyId, "HH vacancy id must not be blank");

        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        log.info("Requesting suitable HH.ru resumes for vacancy: vacancyId={}", normalizedVacancyId);

        return requestExecutor.getAuthorized(
                SUITABLE_RESUMES_PATH_TEMPLATE.formatted(normalizedVacancyId),
                Map.of(),
                accessToken,
                HhSuitableResumesResponse.class
        );
    }

    public CompletableFuture<HhVacancyDetailsResponse> getVacancyDetailsAuthorized(
            String vacancyId,
            String accessToken
    ) {
        String normalizedVacancyId = requireNotBlank(
                vacancyId,
                "HH vacancy id must not be blank"
        );

        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException(
                    "HH API access token must not be blank",
                    -1,
                    null
            );
        }

        log.info(
                "Requesting detailed HH.ru vacancy information: vacancyId={}",
                normalizedVacancyId
        );

        return requestExecutor.getAuthorized(
                VACANCY_DETAILS_PATH_TEMPLATE.formatted(normalizedVacancyId),
                Map.of(),
                accessToken,
                HhVacancyDetailsResponse.class
        );
    }

    public CompletableFuture<HhMineResumesResponse> getMyResumes(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        log.info("Requesting current HH.ru user resumes");

        return requestExecutor.getAuthorized(
                MY_RESUMES_PATH,
                Map.of(),
                accessToken,
                HhMineResumesResponse.class
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

    public CompletableFuture<Void> applyToVacancy(
            String vacancyId,
            HhApplyToVacancyRequest request,
            String accessToken
    ) {
        String normalizedVacancyId = requireNotBlank(vacancyId, "HH vacancy id must not be blank");
        Objects.requireNonNull(request, "HH apply to vacancy request must not be null");

        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        log.info(
                "Applying to HH.ru vacancy through negotiations endpoint: vacancyId={}, resumeProvided={}, messageProvided={}",
                normalizedVacancyId,
                request.resumeId() != null && !request.resumeId().isBlank(),
                request.message() != null && !request.message().isBlank()
        );

        return requestExecutor.postAuthorizedJson(
                NEGOTIATIONS_PATH,
                request,
                accessToken,
                Void.class
        );
    }

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new HhApiRequestException(message, -1, null);
        }

        return value.trim();
    }

    public CompletableFuture<HhVacancySearchResponse> searchVacancies(
            HhVacancySearchRequest request
    ) {
        Objects.requireNonNull(request, "HH vacancy search request must not be null");

        log.info(
                "Searching HH.ru vacancies without authorization: textProvided={}, areaProvided={}, page={}, perPage={}",
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

    public CompletableFuture<HhVacancySearchResponse> searchVacanciesAuthorized(
            HhVacancySearchRequest request,
            String accessToken
    ) {
        Objects.requireNonNull(request, "HH vacancy search request must not be null");

        if (accessToken == null || accessToken.isBlank()) {
            throw new HhApiRequestException("HH API access token must not be blank", -1, null);
        }

        log.info(
                "Searching HH.ru vacancies with application authorization: textProvided={}, areaProvided={}, page={}, perPage={}",
                request.text() != null && !request.text().isBlank(),
                request.area() != null && !request.area().isBlank(),
                request.page(),
                request.perPage()
        );

        return requestExecutor.getAuthorized(
                VACANCIES_PATH,
                request.toQueryParameters(),
                accessToken,
                HhVacancySearchResponse.class
        );
    }
}
