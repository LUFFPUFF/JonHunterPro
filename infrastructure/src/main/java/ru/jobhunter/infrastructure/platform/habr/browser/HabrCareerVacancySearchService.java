package ru.jobhunter.infrastructure.platform.habr.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HabrCareerVacancyFullSearchResultDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchProgressDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchQuery;
import ru.jobhunter.core.application.dto.HabrCareerVacancySearchResultDto;
import ru.jobhunter.core.application.dto.HabrCareerVisibleVacancyDto;
import ru.jobhunter.core.application.usecase.integration.SearchHabrCareerVacanciesUseCase;
import ru.jobhunter.core.domain.model.UserId;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Service
public final class HabrCareerVacancySearchService
        implements SearchHabrCareerVacanciesUseCase {

    static final String VACANCIES_URL = "https://career.habr.com/vacancies";
    static final String VACANCY_CARD_SELECTOR = "[data-vacancy-card]";

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerVacancySearchService.class
    );
    private static final String HABR_CAREER_HOST = "career.habr.com";
    private static final String VACANCIES_PATH = "/vacancies";
    private static final String SSR_STATE_SELECTOR =
            "script[data-ssr-state='true']";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties properties;
    private final HabrCareerVacancyCardExtractor vacancyCardExtractor;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerVacancySearchService(
            HabrCareerBrowserDriverFactory driverFactory,
            HabrCareerBrowserSessionProperties properties,
            HabrCareerVacancyCardExtractor vacancyCardExtractor,
            ObjectMapper objectMapper,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.driverFactory = Objects.requireNonNull(
                driverFactory,
                "Habr Career browser driver factory must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career browser session properties must not be null"
        );
        this.vacancyCardExtractor = Objects.requireNonNull(
                vacancyCardExtractor,
                "Habr Career vacancy card extractor must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Application task executor must not be null"
        );
    }

    @Override
    public CompletableFuture<HabrCareerVacancySearchResultDto> search(
            UserId userId,
            HabrCareerVacancySearchQuery query
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(query, "Habr Career search query must not be null");

        return CompletableFuture.supplyAsync(
                () -> searchPageSynchronously(userId, query),
                executorService
        );
    }

    @Override
    public CompletableFuture<HabrCareerVacancyFullSearchResultDto> searchAll(
            UserId userId,
            HabrCareerVacancySearchQuery query,
            Consumer<HabrCareerVacancySearchProgressDto> progressConsumer
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(query, "Habr Career search query must not be null");
        Objects.requireNonNull(
                progressConsumer,
                "Habr Career progress consumer must not be null"
        );

        HabrCareerVacancySearchQuery firstPageQuery =
                new HabrCareerVacancySearchQuery(query.query(), 1);

        return CompletableFuture.supplyAsync(
                () -> searchAllSynchronously(
                        userId,
                        firstPageQuery,
                        progressConsumer
                ),
                executorService
        );
    }

    private HabrCareerVacancySearchResultDto searchPageSynchronously(
            UserId userId,
            HabrCareerVacancySearchQuery query
    ) {
        WebDriver driver = null;

        try {
            driver = driverFactory.createDriver();
            HabrCareerVacancySearchResultDto result = loadSearchPage(
                    driver,
                    query
            );

            log.info(
                    "Habr Career native vacancy search completed: userId={}, "
                            + "query={}, page={}, status={}, cardCount={}, "
                            + "totalResults={}, totalPages={}, finalUrl={}",
                    userId,
                    query.query(),
                    query.page(),
                    result.status(),
                    result.vacancies().size(),
                    result.totalResults(),
                    result.totalPages(),
                    result.finalUrl()
            );

            return result;
        } catch (RuntimeException exception) {
            throw new HabrCareerVacancySearchException(
                    "Could not search Habr Career vacancies: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver);
        }
    }

    private HabrCareerVacancyFullSearchResultDto searchAllSynchronously(
            UserId userId,
            HabrCareerVacancySearchQuery firstPageQuery,
            Consumer<HabrCareerVacancySearchProgressDto> progressConsumer
    ) {
        WebDriver driver = null;

        try {
            driver = driverFactory.createDriver();

            HabrCareerVacancySearchResultDto firstPage = loadSearchPage(
                    driver,
                    firstPageQuery
            );

            if (!firstPage.isReady()) {
                return unavailableFullSearchResult(firstPage);
            }

            int totalPages = firstPage.totalPages();
            String finalUrl = firstPage.finalUrl();
            LinkedHashMap<String, HabrCareerVisibleVacancyDto> vacanciesById =
                    new LinkedHashMap<>();

            appendDistinctVacancies(vacanciesById, firstPage.vacancies());

            int loadedPages = totalPages == 0 ? 0 : 1;
            publishProgress(
                    progressConsumer,
                    loadedPages,
                    totalPages,
                    vacanciesById.size()
            );

            for (int page = 2; page <= totalPages; page++) {
                HabrCareerVacancySearchResultDto pageResult = loadSearchPage(
                        driver,
                        new HabrCareerVacancySearchQuery(
                                firstPageQuery.query(),
                                page
                        )
                );

                if (!pageResult.isReady()) {
                    log.warn(
                            "Habr Career full search stopped at page {}: "
                                    + "status={}, query={}, userId={}",
                            page,
                            pageResult.status(),
                            firstPageQuery.query(),
                            userId
                    );

                    return new HabrCareerVacancyFullSearchResultDto(
                            HabrCareerVacancyFullSearchResultDto.Status
                                    .PARTIAL_PAGES_LOADED,
                            pageResult.finalUrl(),
                            List.copyOf(vacanciesById.values()),
                            firstPage.totalResults(),
                            loadedPages,
                            totalPages,
                            page,
                            Instant.now()
                    );
                }

                finalUrl = pageResult.finalUrl();
                appendDistinctVacancies(vacanciesById, pageResult.vacancies());
                loadedPages++;

                publishProgress(
                        progressConsumer,
                        loadedPages,
                        totalPages,
                        vacanciesById.size()
                );
            }

            HabrCareerVacancyFullSearchResultDto result =
                    new HabrCareerVacancyFullSearchResultDto(
                            HabrCareerVacancyFullSearchResultDto.Status
                                    .ALL_PAGES_LOADED,
                            finalUrl,
                            List.copyOf(vacanciesById.values()),
                            firstPage.totalResults(),
                            loadedPages,
                            totalPages,
                            null,
                            Instant.now()
                    );

            log.info(
                    "Habr Career full vacancy search completed: userId={}, "
                            + "query={}, loadedPages={}, totalPages={}, "
                            + "vacancyCount={}, totalResults={}",
                    userId,
                    firstPageQuery.query(),
                    result.loadedPages(),
                    result.totalPages(),
                    result.vacancies().size(),
                    result.totalResults()
            );

            return result;
        } catch (RuntimeException exception) {
            throw new HabrCareerVacancySearchException(
                    "Could not load all Habr Career vacancy pages: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver);
        }
    }

    private HabrCareerVacancySearchResultDto loadSearchPage(
            WebDriver driver,
            HabrCareerVacancySearchQuery query
    ) {
        driver.get(buildSearchUrl(query));
        waitUntilDocumentIsReady(driver);

        String finalUrl = normalize(driver.getCurrentUrl());
        int cardCount = isVacanciesPage(finalUrl)
                ? waitForVacancyCardsOrEmptyResult(driver)
                : 0;

        SearchMetadata metadata = readSearchMetadata(
                driver,
                query.page(),
                cardCount
        );

        HabrCareerVacancySearchResultDto.Status status = resolveStatus(
                finalUrl,
                cardCount,
                metadata.totalResults()
        );

        List<HabrCareerVisibleVacancyDto> vacancies = status
                == HabrCareerVacancySearchResultDto.Status.VACANCIES_READY
                ? vacancyCardExtractor.extractVisibleVacancies(driver)
                : List.of();

        return new HabrCareerVacancySearchResultDto(
                status,
                finalUrl,
                vacancies,
                metadata.totalResults(),
                metadata.perPage(),
                metadata.currentPage(),
                metadata.totalPages(),
                Instant.now()
        );
    }

    private static HabrCareerVacancyFullSearchResultDto
    unavailableFullSearchResult(
            HabrCareerVacancySearchResultDto result
    ) {
        return new HabrCareerVacancyFullSearchResultDto(
                mapUnavailableStatus(result.status()),
                result.finalUrl(),
                List.of(),
                result.totalResults(),
                0,
                result.totalPages(),
                null,
                Instant.now()
        );
    }

    private static HabrCareerVacancyFullSearchResultDto.Status
    mapUnavailableStatus(HabrCareerVacancySearchResultDto.Status status) {
        return switch (status) {
            case AUTHENTICATION_REQUIRED ->
                    HabrCareerVacancyFullSearchResultDto.Status
                            .AUTHENTICATION_REQUIRED;
            case VACANCY_CARDS_NOT_FOUND ->
                    HabrCareerVacancyFullSearchResultDto.Status
                            .VACANCY_CARDS_NOT_FOUND;
            case UNEXPECTED_PAGE ->
                    HabrCareerVacancyFullSearchResultDto.Status
                            .UNEXPECTED_PAGE;
            case VACANCIES_READY -> throw new IllegalArgumentException(
                    "Ready page result must not be converted to unavailable result"
            );
        };
    }

    private static void appendDistinctVacancies(
            Map<String, HabrCareerVisibleVacancyDto> vacanciesById,
            List<HabrCareerVisibleVacancyDto> vacancies
    ) {
        for (HabrCareerVisibleVacancyDto vacancy : vacancies) {
            vacanciesById.putIfAbsent(vacancy.externalVacancyId(), vacancy);
        }
    }

    private static void publishProgress(
            Consumer<HabrCareerVacancySearchProgressDto> progressConsumer,
            int loadedPages,
            int totalPages,
            int loadedVacancyCount
    ) {
        try {
            progressConsumer.accept(new HabrCareerVacancySearchProgressDto(
                    loadedPages,
                    totalPages,
                    loadedVacancyCount
            ));
        } catch (RuntimeException exception) {
            log.warn(
                    "Habr Career search progress consumer failed; "
                            + "continuing browser search",
                    exception
            );
        }
    }

    static String buildSearchUrl(HabrCareerVacancySearchQuery query) {
        Objects.requireNonNull(query, "Habr Career search query must not be null");

        StringBuilder url = new StringBuilder(VACANCIES_URL)
                .append("?type=all");

        if (!query.query().isBlank()) {
            url.append("&q=")
                    .append(encodeQueryValue(query.query()));
        }

        if (query.page() > 1) {
            url.append("&page=").append(query.page());
        }

        return url.toString();
    }

    static HabrCareerVacancySearchResultDto.Status resolveStatus(
            String finalUrl,
            int vacancyCardCount,
            int totalResults
    ) {
        URI uri;

        try {
            uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career browser final URL must not be blank"
            ));
        } catch (IllegalArgumentException exception) {
            return HabrCareerVacancySearchResultDto.Status.UNEXPECTED_PAGE;
        }

        String host = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
        String path = normalize(uri.getPath()).toLowerCase(Locale.ROOT);

        if (isAuthenticationPage(host, path)) {
            return HabrCareerVacancySearchResultDto.Status.AUTHENTICATION_REQUIRED;
        }

        if (!HABR_CAREER_HOST.equals(host)
                || !VACANCIES_PATH.equals(path)) {
            return HabrCareerVacancySearchResultDto.Status.UNEXPECTED_PAGE;
        }

        if (vacancyCardCount > 0 || totalResults == 0) {
            return HabrCareerVacancySearchResultDto.Status.VACANCIES_READY;
        }

        return HabrCareerVacancySearchResultDto.Status.VACANCY_CARDS_NOT_FOUND;
    }

    static SearchMetadata parseSearchMetadata(
            String ssrState,
            ObjectMapper objectMapper,
            int fallbackPage,
            int fallbackCardCount
    ) {
        Objects.requireNonNull(objectMapper, "Object mapper must not be null");

        int safeFallbackPage = Math.max(fallbackPage, 1);
        int safeFallbackCardCount = Math.max(fallbackCardCount, 0);

        if (ssrState == null || ssrState.isBlank()) {
            return new SearchMetadata(
                    safeFallbackCardCount,
                    safeFallbackCardCount,
                    safeFallbackPage,
                    safeFallbackCardCount == 0 ? 0 : safeFallbackPage
            );
        }

        try {
            JsonNode meta = objectMapper.readTree(ssrState)
                    .path("vacancies")
                    .path("meta");

            int totalResults = nonNegative(
                    meta.path("totalResults").asInt(safeFallbackCardCount)
            );
            int perPage = nonNegative(
                    meta.path("perPage").asInt(safeFallbackCardCount)
            );
            int totalPages = nonNegative(
                    meta.path("totalPages").asInt(
                            totalResults == 0 ? 0 : safeFallbackPage
                    )
            );
            int currentPage = nonNegative(
                    meta.path("currentPage").asInt(
                            totalPages == 0 ? 0 : safeFallbackPage
                    )
            );

            if (totalPages > 0 && currentPage == 0) {
                currentPage = safeFallbackPage;
            }
            if (totalPages > 0 && currentPage > totalPages) {
                currentPage = totalPages;
            }

            return new SearchMetadata(
                    totalResults,
                    perPage,
                    currentPage,
                    totalPages
            );
        } catch (Exception exception) {
            log.warn(
                    "Could not parse Habr Career SSR pagination metadata; "
                            + "using browser-page fallback",
                    exception
            );

            return new SearchMetadata(
                    safeFallbackCardCount,
                    safeFallbackCardCount,
                    safeFallbackPage,
                    safeFallbackCardCount == 0 ? 0 : safeFallbackPage
            );
        }
    }

    private int waitForVacancyCardsOrEmptyResult(WebDriver driver) {
        try {
            return new WebDriverWait(driver, properties.waitTimeout())
                    .until(webDriver -> {
                        int count = webDriver.findElements(
                                        By.cssSelector(VACANCY_CARD_SELECTOR)
                                )
                                .size();

                        if (count > 0 || hasZeroSearchResults(webDriver)) {
                            return count;
                        }

                        return null;
                    });
        } catch (TimeoutException exception) {
            return 0;
        }
    }

    private boolean hasZeroSearchResults(WebDriver driver) {
        SearchMetadata metadata = readSearchMetadata(driver, 1, 0);
        return metadata.totalResults() == 0;
    }

    private SearchMetadata readSearchMetadata(
            WebDriver driver,
            int fallbackPage,
            int fallbackCardCount
    ) {
        Object rawState = ((JavascriptExecutor) driver).executeScript(
                "var state = document.querySelector(arguments[0]);"
                        + "return state ? state.textContent : '';",
                SSR_STATE_SELECTOR
        );

        return parseSearchMetadata(
                rawState == null ? "" : rawState.toString(),
                objectMapper,
                fallbackPage,
                fallbackCardCount
        );
    }

    private void waitUntilDocumentIsReady(WebDriver driver) {
        new WebDriverWait(driver, properties.waitTimeout())
                .until(webDriver -> "complete".equals(
                        ((JavascriptExecutor) webDriver)
                                .executeScript("return document.readyState")
                ));
    }

    private static boolean isVacanciesPage(String url) {
        try {
            URI uri = URI.create(url);
            return HABR_CAREER_HOST.equalsIgnoreCase(uri.getHost())
                    && VACANCIES_PATH.equals(normalize(uri.getPath()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isAuthenticationPage(String host, String path) {
        if (host.endsWith(".habr.com") && !HABR_CAREER_HOST.equals(host)) {
            return true;
        }

        return HABR_CAREER_HOST.equals(host)
                && (path.contains("/login")
                || path.contains("/sign_in")
                || path.contains("/sign-in")
                || path.contains("/auth"));
    }

    private static String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static int nonNegative(int value) {
        return Math.max(value, 0);
    }

    private static void closeDriver(WebDriver driver) {
        if (driver == null) {
            return;
        }

        try {
            driver.quit();
        } catch (RuntimeException exception) {
            log.warn("Could not close Habr Career browser driver", exception);
        }
    }

    private static String requireNotBlank(String value, String message) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    record SearchMetadata(
            int totalResults,
            int perPage,
            int currentPage,
            int totalPages
    ) {
    }
}
