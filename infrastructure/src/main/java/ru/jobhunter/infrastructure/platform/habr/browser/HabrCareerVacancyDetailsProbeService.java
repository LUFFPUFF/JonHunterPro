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
import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsProbeResultDto;
import ru.jobhunter.core.application.usecase.integration.ProbeHabrCareerVacancyDetailsUseCase;
import ru.jobhunter.core.domain.model.UserId;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public final class HabrCareerVacancyDetailsProbeService
        implements ProbeHabrCareerVacancyDetailsUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerVacancyDetailsProbeService.class
    );
    private static final String HABR_CAREER_HOST = "career.habr.com";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties properties;
    private final HabrCareerVacancyDetailsExtractor detailsExtractor;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerVacancyDetailsProbeService(
            HabrCareerBrowserDriverFactory driverFactory,
            HabrCareerBrowserSessionProperties properties,
            HabrCareerVacancyDetailsExtractor detailsExtractor,
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
        this.detailsExtractor = Objects.requireNonNull(
                detailsExtractor,
                "Habr Career vacancy details extractor must not be null"
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
    public CompletableFuture<HabrCareerVacancyDetailsProbeResultDto> probe(
            UserId userId,
            String externalVacancyId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        String targetUrl = HabrCareerVacancyDetailsExtractor.buildVacancyUrl(
                externalVacancyId
        );

        return CompletableFuture.supplyAsync(
                () -> probeSynchronously(
                        userId,
                        externalVacancyId,
                        targetUrl,
                        UUID.randomUUID()
                ),
                executorService
        );
    }

    private HabrCareerVacancyDetailsProbeResultDto probeSynchronously(
            UserId userId,
            String requestedExternalVacancyId,
            String targetUrl,
            UUID probeId
    ) {
        Path diagnosticDirectory = diagnosticDirectory(userId, probeId);
        WebDriver driver = null;
        String finalUrl = "";
        String pageTitle = "";
        HabrCareerVacancyDetailsDto vacancy = null;
        HabrCareerVacancyDetailsProbeResultDto.Status status = null;
        String diagnosticReason = "habr-vacancy-details-probe-failed";

        try {
            driver = driverFactory.createDriver();
            driver.get(targetUrl);
            waitUntilDocumentIsReady(driver);

            finalUrl = requireNotBlank(
                    driver.getCurrentUrl(),
                    "Habr Career browser did not expose the final vacancy URL"
            );
            pageTitle = normalize(driver.getTitle());

            boolean vacancyRootPresent = false;

            if (HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                    finalUrl,
                    requestedExternalVacancyId
            )) {
                vacancyRootPresent = waitForVacancyRoot(driver);

                if (vacancyRootPresent) {
                    vacancy = detailsExtractor.extract(
                            driver,
                            requestedExternalVacancyId
                    );
                }
            }

            status = resolveStatus(
                    requestedExternalVacancyId,
                    finalUrl,
                    vacancyRootPresent
            );
            diagnosticReason = diagnosticReason(status);

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    targetUrl,
                    finalUrl,
                    pageTitle,
                    status,
                    vacancy
            );

            HabrCareerVacancyDetailsProbeResultDto result =
                    new HabrCareerVacancyDetailsProbeResultDto(
                            status,
                            requestedExternalVacancyId,
                            finalUrl,
                            pageTitle,
                            vacancy,
                            HabrCareerVacancyDetailsExtractor.RESPONSE_ACTION_SELECTOR,
                            diagnosticDirectory.toString(),
                            Instant.now()
                    );

            log.info(
                    "Habr Career vacancy details probe completed: userId={}, "
                            + "probeId={}, requestedVacancyId={}, status={}, "
                            + "responseActionAvailable={}, diagnosticsDirectory={}",
                    userId,
                    probeId,
                    requestedExternalVacancyId,
                    result.status(),
                    vacancy != null && vacancy.responseActionAvailable(),
                    result.diagnosticDirectory()
            );

            return result;
        } catch (RuntimeException exception) {
            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    targetUrl,
                    finalUrl,
                    pageTitle,
                    status,
                    vacancy
            );

            throw new HabrCareerVacancyDetailsProbeException(
                    "Could not probe Habr Career vacancy details: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver, probeId);
        }
    }

    static HabrCareerVacancyDetailsProbeResultDto.Status resolveStatus(
            String requestedExternalVacancyId,
            String finalUrl,
            boolean vacancyRootPresent
    ) {
        URI uri;

        try {
            uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career browser final URL must not be blank"
            ));
        } catch (IllegalArgumentException exception) {
            return HabrCareerVacancyDetailsProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        String host = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
        String path = normalize(uri.getPath()).toLowerCase(Locale.ROOT);

        if (isAuthenticationPage(host, path)) {
            return HabrCareerVacancyDetailsProbeResultDto.Status.AUTHENTICATION_REQUIRED;
        }

        if (!HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                finalUrl,
                requestedExternalVacancyId
        )) {
            return HabrCareerVacancyDetailsProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        return vacancyRootPresent
                ? HabrCareerVacancyDetailsProbeResultDto.Status.VACANCY_DETAILS_READY
                : HabrCareerVacancyDetailsProbeResultDto.Status.VACANCY_DETAILS_NOT_FOUND;
    }

    private boolean waitForVacancyRoot(WebDriver driver) {
        try {
            return new WebDriverWait(driver, properties.waitTimeout())
                    .until(webDriver -> {
                        int count = webDriver.findElements(
                                        By.cssSelector(
                                                HabrCareerVacancyDetailsExtractor
                                                        .VACANCY_ROOT_SELECTOR
                                        )
                                )
                                .size();
                        return count > 0;
                    });
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private void waitUntilDocumentIsReady(WebDriver driver) {
        new WebDriverWait(driver, properties.waitTimeout())
                .until(webDriver -> "complete".equals(
                        ((JavascriptExecutor) webDriver)
                                .executeScript("return document.readyState")
                ));
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
            String targetUrl,
            String finalUrl,
            String pageTitle,
            HabrCareerVacancyDetailsProbeResultDto.Status status,
            HabrCareerVacancyDetailsDto vacancy
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

            VacancyDetailsBrowserDiagnostic diagnostic =
                    new VacancyDetailsBrowserDiagnostic(
                            normalize(reason),
                            normalize(targetUrl),
                            normalize(finalUrl),
                            normalize(pageTitle),
                            status == null ? "FAILED" : status.name(),
                            HabrCareerVacancyDetailsExtractor.VACANCY_ROOT_SELECTOR,
                            HabrCareerVacancyDetailsExtractor.RESPONSE_ACTION_SELECTOR,
                            Instant.now()
                    );

            Files.writeString(
                    diagnosticDirectory.resolve(filePrefix + ".json"),
                    objectMapper.writeValueAsString(diagnostic),
                    StandardCharsets.UTF_8
            );

            VacancyDetailsSnapshotDiagnostic snapshot =
                    new VacancyDetailsSnapshotDiagnostic(
                            normalize(reason),
                            normalize(targetUrl),
                            normalize(finalUrl),
                            vacancy,
                            Instant.now()
                    );

            Files.writeString(
                    diagnosticDirectory.resolve(
                            filePrefix + "-vacancy-details.json"
                    ),
                    objectMapper.writeValueAsString(snapshot),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save Habr Career vacancy details diagnostics: "
                            + "reason={}, diagnosticsDirectory={}",
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
                "vacancy-details-probe",
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
            HabrCareerVacancyDetailsProbeResultDto.Status status
    ) {
        return switch (status) {
            case VACANCY_DETAILS_READY -> "habr-vacancy-details-ready";
            case AUTHENTICATION_REQUIRED ->
                    "habr-vacancy-details-authentication-required";
            case VACANCY_DETAILS_NOT_FOUND ->
                    "habr-vacancy-details-not-found";
            case UNEXPECTED_PAGE -> "habr-vacancy-details-unexpected-page";
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

    private record VacancyDetailsBrowserDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            String status,
            String vacancyRootSelector,
            String responseActionSelector,
            Instant capturedAt
    ) {
    }

    private record VacancyDetailsSnapshotDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            HabrCareerVacancyDetailsDto vacancy,
            Instant capturedAt
    ) {
    }
}
