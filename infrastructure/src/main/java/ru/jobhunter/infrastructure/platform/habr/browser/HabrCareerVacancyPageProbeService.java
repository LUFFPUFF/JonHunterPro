package ru.jobhunter.infrastructure.platform.habr.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HabrCareerVacancyPageProbeResultDto;
import ru.jobhunter.core.application.dto.HabrCareerVisibleVacancyDto;
import ru.jobhunter.core.application.usecase.integration.ProbeHabrCareerVacancyPageUseCase;
import ru.jobhunter.core.domain.model.UserId;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public final class HabrCareerVacancyPageProbeService
        implements ProbeHabrCareerVacancyPageUseCase {

    static final String VACANCIES_URL = "https://career.habr.com/vacancies";
    static final String VACANCY_CARD_SELECTOR = "[data-vacancy-card]";

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerVacancyPageProbeService.class
    );
    private static final String HABR_CAREER_HOST = "career.habr.com";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties properties;
    private final HabrCareerVacancyCardExtractor vacancyCardExtractor;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerVacancyPageProbeService(
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
    public CompletableFuture<HabrCareerVacancyPageProbeResultDto> probe(
            UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return CompletableFuture.supplyAsync(
                () -> probeSynchronously(userId, UUID.randomUUID()),
                executorService
        );
    }

    private HabrCareerVacancyPageProbeResultDto probeSynchronously(
            UserId userId,
            UUID probeId
    ) {
        Path diagnosticDirectory = diagnosticDirectory(userId, probeId);
        WebDriver driver = null;
        String finalUrl = "";
        String pageTitle = "";
        int vacancyCardCount = 0;
        List<HabrCareerVisibleVacancyDto> visibleVacancies = List.of();
        HabrCareerVacancyPageProbeResultDto.Status status = null;
        String diagnosticReason = "habr-vacancies-probe-failed";

        try {
            driver = driverFactory.createDriver();
            driver.get(VACANCIES_URL);
            waitUntilDocumentIsReady(driver);

            finalUrl = requireNotBlank(
                    driver.getCurrentUrl(),
                    "Habr Career browser did not expose the final vacancies page URL"
            );
            pageTitle = normalize(driver.getTitle());

            if (isVacancyListUrl(finalUrl)) {
                vacancyCardCount = waitForVacancyCards(driver);

                if (vacancyCardCount > 0) {
                    visibleVacancies = vacancyCardExtractor.extractVisibleVacancies(
                            driver
                    );
                    vacancyCardCount = visibleVacancies.size();
                }
            }

            status = resolveStatus(finalUrl, vacancyCardCount);
            diagnosticReason = diagnosticReason(status);

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    finalUrl,
                    pageTitle,
                    status,
                    vacancyCardCount,
                    visibleVacancies
            );

            HabrCareerVacancyPageProbeResultDto result =
                    new HabrCareerVacancyPageProbeResultDto(
                            status,
                            finalUrl,
                            pageTitle,
                            vacancyCardCount,
                            VACANCY_CARD_SELECTOR,
                            diagnosticDirectory.toString(),
                            Instant.now()
                    );

            log.info(
                    "Habr Career vacancies page probe completed: userId={}, "
                            + "probeId={}, status={}, finalUrl={}, vacancyCardCount={}, "
                            + "extractedVacancyCount={}, diagnosticsDirectory={}",
                    userId,
                    probeId,
                    result.status(),
                    result.finalUrl(),
                    result.vacancyCardCount(),
                    visibleVacancies.size(),
                    result.diagnosticDirectory()
            );

            return result;
        } catch (RuntimeException exception) {
            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    finalUrl,
                    pageTitle,
                    status,
                    vacancyCardCount,
                    visibleVacancies
            );

            throw new HabrCareerVacancyPageProbeException(
                    "Could not probe Habr Career vacancies page: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver, probeId);
        }
    }

    static HabrCareerVacancyPageProbeResultDto.Status resolveStatus(
            String finalUrl,
            int vacancyCardCount
    ) {
        URI uri;

        try {
            uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career browser final URL must not be blank"
            ));
        } catch (IllegalArgumentException exception) {
            return HabrCareerVacancyPageProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        String host = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
        String path = normalize(uri.getPath()).toLowerCase(Locale.ROOT);

        if (isAuthenticationPage(host, path)) {
            return HabrCareerVacancyPageProbeResultDto.Status.AUTHENTICATION_REQUIRED;
        }

        if (!HABR_CAREER_HOST.equals(host) || !"/vacancies".equals(path)) {
            return HabrCareerVacancyPageProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        return vacancyCardCount > 0
                ? HabrCareerVacancyPageProbeResultDto.Status.VACANCY_LIST_READY
                : HabrCareerVacancyPageProbeResultDto.Status.VACANCY_CARDS_NOT_FOUND;
    }

    private int waitForVacancyCards(WebDriver driver) {
        try {
            return new WebDriverWait(driver, properties.waitTimeout())
                    .until(webDriver -> {
                        int count = webDriver.findElements(
                                        By.cssSelector(VACANCY_CARD_SELECTOR)
                                )
                                .size();
                        return count > 0 ? count : null;
                    });
        } catch (TimeoutException exception) {
            return 0;
        }
    }

    private void waitUntilDocumentIsReady(WebDriver driver) {
        new WebDriverWait(driver, properties.waitTimeout())
                .until(webDriver -> "complete".equals(
                        ((JavascriptExecutor) webDriver)
                                .executeScript("return document.readyState")
                ));
    }

    private static boolean isVacancyListUrl(String finalUrl) {
        try {
            URI uri = URI.create(finalUrl);
            return HABR_CAREER_HOST.equalsIgnoreCase(uri.getHost())
                    && "/vacancies".equals(normalize(uri.getPath()));
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
                || path.contains("/auth/"));
    }

    private void saveDiagnostics(
            WebDriver driver,
            Path diagnosticDirectory,
            String reason,
            String finalUrl,
            String pageTitle,
            HabrCareerVacancyPageProbeResultDto.Status status,
            int vacancyCardCount,
            List<HabrCareerVisibleVacancyDto> visibleVacancies
    ) {
        try {
            Files.createDirectories(diagnosticDirectory);
            String filePrefix = safePathSegment(reason)
                    + "-"
                    + Instant.now().toString()
                    .replace(":", "-")
                    .replace(".", "-");

            if (driver != null) {
                Files.writeString(
                        diagnosticDirectory.resolve(filePrefix + ".html"),
                        Objects.requireNonNullElse(driver.getPageSource(), ""),
                        StandardCharsets.UTF_8
                );

                if (driver instanceof TakesScreenshot screenshotDriver) {
                    FileHandler.copy(
                            screenshotDriver.getScreenshotAs(OutputType.FILE),
                            diagnosticDirectory.resolve(filePrefix + ".png").toFile()
                    );
                }
            }

            BrowserDiagnostic diagnostic = new BrowserDiagnostic(
                    normalize(reason),
                    VACANCIES_URL,
                    normalize(finalUrl),
                    normalize(pageTitle),
                    status == null ? "FAILED" : status.name(),
                    vacancyCardCount,
                    VACANCY_CARD_SELECTOR,
                    visibleVacancies.size(),
                    Instant.now()
            );

            Files.writeString(
                    diagnosticDirectory.resolve(filePrefix + ".json"),
                    objectMapper.writeValueAsString(diagnostic),
                    StandardCharsets.UTF_8
            );

            VacancySnapshotDiagnostic snapshot = new VacancySnapshotDiagnostic(
                    normalize(reason),
                    VACANCIES_URL,
                    normalize(finalUrl),
                    normalize(pageTitle),
                    VACANCY_CARD_SELECTOR,
                    List.copyOf(visibleVacancies),
                    Instant.now()
            );

            Files.writeString(
                    diagnosticDirectory.resolve(
                            filePrefix + "-visible-vacancies.json"
                    ),
                    objectMapper.writeValueAsString(snapshot),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save Habr Career vacancies diagnostics: reason={}, "
                            + "diagnosticsDirectory={}",
                    reason,
                    diagnosticDirectory,
                    exception
            );
        }
    }

    private Path diagnosticDirectory(UserId userId, UUID probeId) {
        return Path.of(
                "logs",
                "habr-browser-debug",
                "vacancy-page-probe",
                safePathSegment(userId.toString()),
                probeId.toString()
        ).toAbsolutePath().normalize();
    }

    private void closeDriver(WebDriver driver, UUID probeId) {
        if (driver == null) {
            return;
        }

        try {
            driver.quit();
        } catch (RuntimeException exception) {
            log.warn(
                    "Habr Career browser driver could not be closed: probeId={}",
                    probeId,
                    exception
            );
        }
    }

    private static String diagnosticReason(
            HabrCareerVacancyPageProbeResultDto.Status status
    ) {
        return switch (status) {
            case VACANCY_LIST_READY -> "habr-vacancies-list-ready";
            case AUTHENTICATION_REQUIRED ->
                    "habr-vacancies-authentication-required";
            case VACANCY_CARDS_NOT_FOUND -> "habr-vacancies-cards-not-found";
            case UNEXPECTED_PAGE -> "habr-vacancies-unexpected-page";
        };
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

    private static String safePathSegment(String value) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            return "unknown";
        }

        String sanitized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "unknown" : sanitized;
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

    private record BrowserDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            String status,
            int vacancyCardCount,
            String vacancyCardSelector,
            int extractedVacancyCount,
            Instant capturedAt
    ) {
    }

    private record VacancySnapshotDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            String vacancyCardSelector,
            List<HabrCareerVisibleVacancyDto> vacancies,
            Instant capturedAt
    ) {
    }
}
