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
import ru.jobhunter.core.application.dto.HabrCareerPostResponseFormProbeResultDto;
import ru.jobhunter.core.application.dto.HabrCareerResponseFormControlDto;
import ru.jobhunter.core.application.usecase.integration.ProbeHabrCareerPostResponseFormUseCase;
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
public final class HabrCareerPostResponseFormProbeService
        implements ProbeHabrCareerPostResponseFormUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerPostResponseFormProbeService.class
    );
    private static final String HABR_CAREER_HOST = "career.habr.com";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties properties;
    private final HabrCareerResponseFormExtractor responseFormExtractor;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerPostResponseFormProbeService(
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
    public CompletableFuture<HabrCareerPostResponseFormProbeResultDto> probe(
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

    private HabrCareerPostResponseFormProbeResultDto probeSynchronously(
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
        boolean responseExistsEditable = false;
        String terminalResponseMarker = "";
        boolean postResponseFormAvailable = false;
        List<HabrCareerResponseFormControlDto> controls = List.of();
        List<String> visibleButtonLabels = List.of();
        HabrCareerPostResponseFormProbeResultDto.Status status = null;
        String diagnosticReason = "habr-post-response-form-probe-failed";

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
                terminalResponseMarker = responseFormExtractor
                        .terminalResponseMarker(driver);
                initialResponseActionAvailable = responseFormExtractor
                        .hasInitialResponseAction(driver);
                responseExistsEditable = responseFormExtractor
                        .hasExistingEditableResponse(driver);
                postResponseFormAvailable = responseFormExtractor
                        .hasPostResponseForm(driver);

                if (postResponseFormAvailable || responseExistsEditable) {
                    controls = responseFormExtractor.extractControls(driver);
                    visibleButtonLabels = responseFormExtractor
                            .extractVisibleButtonLabels(driver);
                }
            }

            status = resolveStatus(
                    requestedExternalVacancyId,
                    finalUrl,
                    vacancyRootPresent,
                    terminalResponseMarker,
                    postResponseFormAvailable,
                    responseExistsEditable,
                    initialResponseActionAvailable
            );
            diagnosticReason = diagnosticReason(status);

            HabrCareerPostResponseFormProbeResultDto result = result(
                    status,
                    requestedExternalVacancyId,
                    finalUrl,
                    pageTitle,
                    terminalResponseMarker,
                    responseExistsEditable,
                    postResponseFormAvailable,
                    controls,
                    visibleButtonLabels,
                    diagnosticDirectory
            );

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    targetUrl,
                    result
            );

            log.info(
                    "Habr Career post-response form probe completed: userId={}, "
                            + "probeId={}, requestedVacancyId={}, status={}, "
                            + "responseExistsEditable={}, postResponseFormAvailable={}, diagnosticsDirectory={}",
                    userId,
                    probeId,
                    requestedExternalVacancyId,
                    result.status(),
                    result.responseExistsEditable(),
                    result.postResponseFormAvailable(),
                    result.diagnosticDirectory()
            );

            return result;
        } catch (RuntimeException exception) {
            HabrCareerPostResponseFormProbeResultDto failedResult = result(
                    status == null
                            ? HabrCareerPostResponseFormProbeResultDto.Status
                            .UNEXPECTED_PAGE
                            : status,
                    requestedExternalVacancyId,
                    finalUrl,
                    pageTitle,
                    terminalResponseMarker,
                    responseExistsEditable,
                    postResponseFormAvailable,
                    controls,
                    visibleButtonLabels,
                    diagnosticDirectory
            );

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    targetUrl,
                    failedResult
            );

            throw new HabrCareerPostResponseFormProbeException(
                    "Could not probe Habr Career post-response form: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver, probeId);
        }
    }

    static HabrCareerPostResponseFormProbeResultDto.Status resolveStatus(
            String requestedExternalVacancyId,
            String finalUrl,
            boolean vacancyRootPresent,
            String terminalResponseMarker,
            boolean postResponseFormAvailable,
            boolean responseExistsEditable,
            boolean initialResponseActionAvailable
    ) {
        URI uri;

        try {
            uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career browser final URL must not be blank"
            ));
        } catch (IllegalArgumentException exception) {
            return HabrCareerPostResponseFormProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        String host = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
        String path = normalize(uri.getPath()).toLowerCase(Locale.ROOT);

        if (isAuthenticationPage(host, path)) {
            return HabrCareerPostResponseFormProbeResultDto.Status
                    .AUTHENTICATION_REQUIRED;
        }

        if (!HabrCareerVacancyDetailsExtractor.isExpectedVacancyUrl(
                finalUrl,
                requestedExternalVacancyId
        )) {
            return HabrCareerPostResponseFormProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        if (!vacancyRootPresent) {
            return HabrCareerPostResponseFormProbeResultDto.Status
                    .VACANCY_DETAILS_NOT_FOUND;
        }

        if (postResponseFormAvailable) {
            return HabrCareerPostResponseFormProbeResultDto.Status
                    .POST_RESPONSE_FORM_READY;
        }

        if (responseExistsEditable) {
            return HabrCareerPostResponseFormProbeResultDto.Status
                    .RESPONSE_EXISTS_EDITABLE;
        }

        if (!normalize(terminalResponseMarker).isBlank()) {
            return HabrCareerPostResponseFormProbeResultDto.Status
                    .TERMINAL_RESPONSE_STATE_WITHOUT_COMPLEMENT_FORM;
        }

        if (initialResponseActionAvailable) {
            return HabrCareerPostResponseFormProbeResultDto.Status
                    .INITIAL_RESPONSE_ACTION_STILL_AVAILABLE;
        }

        return HabrCareerPostResponseFormProbeResultDto.Status.NO_RESPONSE_STATE;
    }

    private HabrCareerPostResponseFormProbeResultDto result(
            HabrCareerPostResponseFormProbeResultDto.Status status,
            String requestedExternalVacancyId,
            String finalUrl,
            String pageTitle,
            String terminalResponseMarker,
            boolean responseExistsEditable,
            boolean postResponseFormAvailable,
            List<HabrCareerResponseFormControlDto> controls,
            List<String> visibleButtonLabels,
            Path diagnosticDirectory
    ) {
        return new HabrCareerPostResponseFormProbeResultDto(
                status,
                requestedExternalVacancyId,
                finalUrl,
                pageTitle,
                terminalResponseMarker,
                responseExistsEditable,
                HabrCareerResponseFormExtractor.EXISTING_RESPONSE_EDIT_ACTION_SELECTOR,
                postResponseFormAvailable,
                HabrCareerResponseFormExtractor.POST_RESPONSE_FORM_SELECTOR,
                HabrCareerResponseFormExtractor.POST_RESPONSE_COVER_LETTER_SELECTOR,
                HabrCareerResponseFormExtractor
                        .POST_RESPONSE_COMPLEMENT_SUBMIT_SELECTOR,
                controls,
                visibleButtonLabels,
                diagnosticDirectory.toString(),
                Instant.now()
        );
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
            HabrCareerPostResponseFormProbeResultDto result
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

            PostResponseFormBrowserDiagnostic diagnostic =
                    new PostResponseFormBrowserDiagnostic(
                            normalize(reason),
                            normalize(targetUrl),
                            result.finalUrl(),
                            result.pageTitle(),
                            result.status().name(),
                            result.terminalResponseMarker(),
                            result.responseExistsEditable(),
                            result.responseEditActionSelector(),
                            result.postResponseFormAvailable(),
                            result.postResponseFormSelector(),
                            result.coverLetterSelector(),
                            result.complementSubmitSelector(),
                            Instant.now()
                    );

            Files.writeString(
                    diagnosticDirectory.resolve(filePrefix + ".json"),
                    objectMapper.writeValueAsString(diagnostic),
                    StandardCharsets.UTF_8
            );

            PostResponseFormSnapshotDiagnostic snapshot =
                    new PostResponseFormSnapshotDiagnostic(
                            normalize(reason),
                            normalize(targetUrl),
                            result.requestedExternalVacancyId(),
                            result.terminalResponseMarker(),
                            result.responseExistsEditable(),
                            result.controls(),
                            result.visibleButtonLabels(),
                            Instant.now()
                    );

            Files.writeString(
                    diagnosticDirectory.resolve(
                            filePrefix + "-post-response-form.json"
                    ),
                    objectMapper.writeValueAsString(snapshot),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save Habr Career post-response form diagnostics: "
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
                "post-response-form-probe",
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
            HabrCareerPostResponseFormProbeResultDto.Status status
    ) {
        return switch (status) {
            case POST_RESPONSE_FORM_READY -> "habr-post-response-form-ready";
            case RESPONSE_EXISTS_EDITABLE -> "habr-response-exists-editable";
            case TERMINAL_RESPONSE_STATE_WITHOUT_COMPLEMENT_FORM ->
                    "habr-terminal-response-without-complement-form";
            case INITIAL_RESPONSE_ACTION_STILL_AVAILABLE ->
                    "habr-initial-response-action-still-available";
            case NO_RESPONSE_STATE -> "habr-no-response-state";
            case AUTHENTICATION_REQUIRED ->
                    "habr-post-response-form-authentication-required";
            case VACANCY_DETAILS_NOT_FOUND ->
                    "habr-post-response-form-vacancy-details-not-found";
            case UNEXPECTED_PAGE -> "habr-post-response-form-unexpected-page";
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

    private record PostResponseFormBrowserDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            String status,
            String terminalResponseMarker,
            boolean responseExistsEditable,
            String responseEditActionSelector,
            boolean postResponseFormAvailable,
            String postResponseFormSelector,
            String coverLetterSelector,
            String complementSubmitSelector,
            Instant capturedAt
    ) {
    }

    private record PostResponseFormSnapshotDiagnostic(
            String reason,
            String targetUrl,
            String requestedExternalVacancyId,
            String terminalResponseMarker,
            boolean responseExistsEditable,
            List<HabrCareerResponseFormControlDto> controls,
            List<String> visibleButtonLabels,
            Instant capturedAt
    ) {
    }
}
