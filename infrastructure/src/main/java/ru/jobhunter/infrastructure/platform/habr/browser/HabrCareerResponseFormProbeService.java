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
import ru.jobhunter.core.application.dto.HabrCareerResponseFormControlDto;
import ru.jobhunter.core.application.dto.HabrCareerResponseFormProbeResultDto;
import ru.jobhunter.core.application.usecase.integration.ProbeHabrCareerResponseFormUseCase;
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
public final class HabrCareerResponseFormProbeService
        implements ProbeHabrCareerResponseFormUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerResponseFormProbeService.class
    );
    private static final String HABR_CAREER_HOST = "career.habr.com";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties properties;
    private final HabrCareerResponseFormExtractor responseFormExtractor;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerResponseFormProbeService(
            HabrCareerBrowserDriverFactory driverFactory,
            HabrCareerBrowserSessionProperties properties,
            HabrCareerResponseFormExtractor responseFormExtractor,
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
        this.responseFormExtractor = Objects.requireNonNull(
                responseFormExtractor,
                "Habr Career response form extractor must not be null"
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
    public CompletableFuture<HabrCareerResponseFormProbeResultDto> probe(
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

    private HabrCareerResponseFormProbeResultDto probeSynchronously(
            UserId userId,
            String requestedExternalVacancyId,
            String targetUrl,
            UUID probeId
    ) {
        Path diagnosticDirectory = diagnosticDirectory(userId, probeId);
        WebDriver driver = null;
        String finalUrl = "";
        String pageTitle = "";
        boolean vacancyRootPresent = false;
        boolean initialResponseActionAvailable = false;
        boolean initialResponseActionClicked = false;
        boolean directResponseActionDetected = false;
        List<HabrCareerResponseFormControlDto> controls = List.of();
        List<String> visibleButtonLabels = List.of();
        HabrCareerResponseFormProbeResultDto.Status status = null;
        String diagnosticReason = "habr-response-form-probe-failed";

        try {
            driver = driverFactory.createDriver();
            driver.get(targetUrl);
            waitUntilDocumentIsReady(driver);

            finalUrl = normalize(driver.getCurrentUrl());
            pageTitle = normalize(driver.getTitle());

            if (HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                    finalUrl,
                    requestedExternalVacancyId
            )) {
                vacancyRootPresent = waitForVacancyRoot(driver);
            }

            if (vacancyRootPresent) {
                initialResponseActionAvailable = responseFormExtractor
                        .hasInitialResponseAction(driver);
                directResponseActionDetected = initialResponseActionAvailable
                        && responseFormExtractor.isDirectResponseAction(driver);
                visibleButtonLabels = responseFormExtractor
                        .extractVisibleButtonLabels(driver);
            }

            status = resolveStatus(
                    requestedExternalVacancyId,
                    finalUrl,
                    vacancyRootPresent,
                    initialResponseActionAvailable,
                    directResponseActionDetected
            );
            diagnosticReason = diagnosticReason(status);

            HabrCareerResponseFormProbeResultDto result =
                    new HabrCareerResponseFormProbeResultDto(
                            status,
                            requestedExternalVacancyId,
                            finalUrl,
                            pageTitle,
                            initialResponseActionClicked,
                            HabrCareerResponseFormExtractor
                                    .INITIAL_RESPONSE_ACTION_SELECTOR,
                            HabrCareerResponseFormExtractor
                                    .RESPONSE_CONTAINER_SELECTOR,
                            controls,
                            visibleButtonLabels,
                            diagnosticDirectory.toString(),
                            Instant.now()
                    );

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    targetUrl,
                    result
            );

            log.info(
                    "Habr Career response-action safety probe completed: userId={}, "
                            + "probeId={}, requestedVacancyId={}, status={}, "
                            + "initialResponseActionClicked={}, diagnosticsDirectory={}",
                    userId,
                    probeId,
                    requestedExternalVacancyId,
                    result.status(),
                    result.initialResponseActionClicked(),
                    result.diagnosticDirectory()
            );

            return result;
        } catch (RuntimeException exception) {
            HabrCareerResponseFormProbeResultDto failedResult =
                    new HabrCareerResponseFormProbeResultDto(
                            status == null
                                    ? HabrCareerResponseFormProbeResultDto.Status
                                    .UNEXPECTED_PAGE
                                    : status,
                            requestedExternalVacancyId,
                            finalUrl,
                            pageTitle,
                            initialResponseActionClicked,
                            HabrCareerResponseFormExtractor
                                    .INITIAL_RESPONSE_ACTION_SELECTOR,
                            HabrCareerResponseFormExtractor
                                    .RESPONSE_CONTAINER_SELECTOR,
                            controls,
                            visibleButtonLabels,
                            diagnosticDirectory.toString(),
                            Instant.now()
                    );

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    targetUrl,
                    failedResult
            );

            throw new HabrCareerResponseFormProbeException(
                    "Could not probe Habr Career response form: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver, probeId);
        }
    }

    static HabrCareerResponseFormProbeResultDto.Status resolveStatus(
            String requestedExternalVacancyId,
            String finalUrl,
            boolean vacancyRootPresent,
            boolean initialResponseActionAvailable,
            boolean directResponseActionDetected
    ) {
        URI uri;

        try {
            uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career browser final URL must not be blank"
            ));
        } catch (IllegalArgumentException exception) {
            return HabrCareerResponseFormProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        String host = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
        String path = normalize(uri.getPath()).toLowerCase(Locale.ROOT);

        if (isAuthenticationPage(host, path)) {
            return HabrCareerResponseFormProbeResultDto.Status
                    .AUTHENTICATION_REQUIRED;
        }

        if (!HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                finalUrl,
                requestedExternalVacancyId
        )) {
            return HabrCareerResponseFormProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        if (!vacancyRootPresent) {
            return HabrCareerResponseFormProbeResultDto.Status
                    .VACANCY_DETAILS_NOT_FOUND;
        }

        if (!initialResponseActionAvailable) {
            return HabrCareerResponseFormProbeResultDto.Status
                    .INITIAL_RESPONSE_ACTION_NOT_AVAILABLE;
        }

        return directResponseActionDetected
                ? HabrCareerResponseFormProbeResultDto.Status
                .DIRECT_RESPONSE_WOULD_SEND_IMMEDIATELY
                : HabrCareerResponseFormProbeResultDto.Status
                .RESPONSE_ACTION_PRESENT_NOT_CLICKED_FOR_SAFETY;
    }

    private boolean waitForVacancyRoot(WebDriver driver) {
        try {
            return new WebDriverWait(driver, properties.waitTimeout())
                    .until(webDriver -> !webDriver.findElements(
                            By.cssSelector(
                                    HabrCareerVacancyDetailsExtractor
                                            .VACANCY_ROOT_SELECTOR
                            )
                    ).isEmpty());
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private void waitUntilDocumentIsReady(WebDriver driver) {
        new WebDriverWait(driver, properties.waitTimeout())
                .until(webDriver -> "complete".equals(
                        ((JavascriptExecutor) webDriver).executeScript(
                                "return document.readyState"
                        )
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
            HabrCareerResponseFormProbeResultDto result
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
                            diagnosticDirectory.resolve(filePrefix + ".png")
                                    .toFile()
                    );
                }
            }

            ResponseFormBrowserDiagnostic diagnostic =
                    new ResponseFormBrowserDiagnostic(
                            normalize(reason),
                            normalize(targetUrl),
                            result.finalUrl(),
                            result.pageTitle(),
                            result.status().name(),
                            result.initialResponseActionClicked(),
                            result.initialResponseActionSelector(),
                            result.responseContainerSelector(),
                            Instant.now()
                    );

            Files.writeString(
                    diagnosticDirectory.resolve(filePrefix + ".json"),
                    objectMapper.writeValueAsString(diagnostic),
                    StandardCharsets.UTF_8
            );

            ResponseFormSnapshotDiagnostic snapshot =
                    new ResponseFormSnapshotDiagnostic(
                            normalize(reason),
                            normalize(targetUrl),
                            result.requestedExternalVacancyId(),
                            result.controls(),
                            result.visibleButtonLabels(),
                            Instant.now()
                    );

            Files.writeString(
                    diagnosticDirectory.resolve(
                            filePrefix + "-response-form.json"
                    ),
                    objectMapper.writeValueAsString(snapshot),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save Habr Career response form diagnostics: "
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
                "response-form-probe",
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
            HabrCareerResponseFormProbeResultDto.Status status
    ) {
        return switch (status) {
            case DIRECT_RESPONSE_WOULD_SEND_IMMEDIATELY ->
                    "habr-direct-response-would-send-immediately";
            case RESPONSE_ACTION_PRESENT_NOT_CLICKED_FOR_SAFETY ->
                    "habr-response-action-present-not-clicked-for-safety";
            case INITIAL_RESPONSE_ACTION_NOT_AVAILABLE ->
                    "habr-response-form-initial-action-not-available";
            case RESPONSE_FORM_READY -> "habr-response-form-ready-legacy";
            case TERMINAL_RESPONSE_STATE_DETECTED ->
                    "habr-response-form-terminal-state-detected-legacy";
            case RESPONSE_FORM_NOT_FOUND ->
                    "habr-response-form-not-found-legacy";
            case AUTHENTICATION_REQUIRED ->
                    "habr-response-form-authentication-required";
            case VACANCY_DETAILS_NOT_FOUND ->
                    "habr-response-form-vacancy-details-not-found";
            case UNEXPECTED_PAGE -> "habr-response-form-unexpected-page";
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

    private record ResponseFormBrowserDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            String status,
            boolean initialResponseActionClicked,
            String initialResponseActionSelector,
            String responseContainerSelector,
            Instant capturedAt
    ) {
    }

    private record ResponseFormSnapshotDiagnostic(
            String reason,
            String targetUrl,
            String requestedExternalVacancyId,
            List<HabrCareerResponseFormControlDto> controls,
            List<String> visibleButtonLabels,
            Instant capturedAt
    ) {
    }
}
