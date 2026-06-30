package ru.jobhunter.infrastructure.platform.habr.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HabrCareerBrowserSessionProbeResultDto;
import ru.jobhunter.core.application.usecase.integration.ProbeHabrCareerBrowserSessionUseCase;
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
public final class HabrCareerBrowserSessionProbeService
        implements ProbeHabrCareerBrowserSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerBrowserSessionProbeService.class
    );

    private static final String PROFILE_URL = "https://career.habr.com/profile";
    private static final String HABR_CAREER_HOST = "career.habr.com";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public HabrCareerBrowserSessionProbeService(
            HabrCareerBrowserDriverFactory driverFactory,
            HabrCareerBrowserSessionProperties properties,
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
    public CompletableFuture<HabrCareerBrowserSessionProbeResultDto> probe(
            UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        UUID probeId = UUID.randomUUID();

        return CompletableFuture.supplyAsync(
                () -> probeSynchronously(userId, probeId),
                executorService
        );
    }

    private HabrCareerBrowserSessionProbeResultDto probeSynchronously(
            UserId userId,
            UUID probeId
    ) {
        Path diagnosticDirectory = diagnosticDirectory(userId, probeId);
        WebDriver driver = null;
        String finalUrl = "";
        String pageTitle = "";
        HabrCareerBrowserSessionProbeResultDto.Status status = null;
        String diagnosticReason = "browser-session-probe-failed";

        try {
            driver = driverFactory.createDriver();
            driver.get(PROFILE_URL);
            waitUntilDocumentIsReady(driver);

            finalUrl = requireNotBlank(
                    driver.getCurrentUrl(),
                    "Habr Career browser did not expose the final page URL"
            );
            pageTitle = normalize(driver.getTitle());
            status = resolveStatus(finalUrl);

            if (status == HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATION_REQUIRED
                    && !properties.headless()) {
                waitForInteractiveLogin(driver);
                finalUrl = requireNotBlank(
                        driver.getCurrentUrl(),
                        "Habr Career browser did not expose the final page URL after login"
                );
                pageTitle = normalize(driver.getTitle());
                status = resolveStatus(finalUrl);
            }

            diagnosticReason = switch (status) {
                case AUTHENTICATED -> "browser-session-authenticated";
                case AUTHENTICATION_REQUIRED -> "browser-session-authentication-required";
                case UNEXPECTED_PAGE -> "browser-session-unexpected-page";
            };

            saveDiagnostics(
                    driver,
                    diagnosticDirectory,
                    diagnosticReason,
                    finalUrl,
                    pageTitle,
                    status
            );

            HabrCareerBrowserSessionProbeResultDto result =
                    new HabrCareerBrowserSessionProbeResultDto(
                            status,
                            finalUrl,
                            pageTitle,
                            diagnosticDirectory.toString(),
                            Instant.now()
                    );

            log.info(
                    "Habr Career browser session probe completed: userId={}, "
                            + "probeId={}, status={}, finalUrl={}, diagnosticsDirectory={}",
                    userId,
                    probeId,
                    result.status(),
                    result.finalUrl(),
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
                    status
            );

            throw new HabrCareerBrowserSessionProbeException(
                    "Could not probe Habr Career browser session: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            closeDriver(driver, probeId);
        }
    }

    static HabrCareerBrowserSessionProbeResultDto.Status resolveStatus(
            String finalUrl
    ) {
        URI uri;

        try {
            uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career browser final URL must not be blank"
            ));
        } catch (IllegalArgumentException exception) {
            return HabrCareerBrowserSessionProbeResultDto.Status.UNEXPECTED_PAGE;
        }

        String normalizedHost = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
        String normalizedPath = normalize(uri.getPath()).toLowerCase(Locale.ROOT);

        if (isAuthenticationPage(normalizedHost, normalizedPath)) {
            return HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATION_REQUIRED;
        }

        if (HABR_CAREER_HOST.equals(normalizedHost)) {
            return HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATED;
        }

        return HabrCareerBrowserSessionProbeResultDto.Status.UNEXPECTED_PAGE;
    }

    private static boolean isAuthenticationPage(
            String host,
            String path
    ) {
        if (host.endsWith(".habr.com")
                && !HABR_CAREER_HOST.equals(host)) {
            return true;
        }

        return HABR_CAREER_HOST.equals(host)
                && (path.contains("/login")
                || path.contains("/sign_in")
                || path.contains("/sign-in")
                || path.contains("/auth/"));
    }

    private void waitUntilDocumentIsReady(WebDriver driver) {
        new WebDriverWait(driver, properties.waitTimeout())
                .until(webDriver -> {
                    Object readyState = ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState");
                    return "complete".equals(readyState);
                });
    }

    private void waitForInteractiveLogin(WebDriver driver) {
        log.info(
                "Habr Career browser session requires interactive login. "
                        + "Waiting up to {} seconds.",
                properties.interactiveLoginTimeout().toSeconds()
        );

        long deadline = System.nanoTime()
                + properties.interactiveLoginTimeout().toNanos();

        while (System.nanoTime() < deadline) {
            HabrCareerBrowserSessionProbeResultDto.Status currentStatus =
                    resolveStatus(driver.getCurrentUrl());

            if (currentStatus != HabrCareerBrowserSessionProbeResultDto.Status.AUTHENTICATION_REQUIRED) {
                waitUntilDocumentIsReady(driver);
                return;
            }

            sleep(500);
        }
    }

    private void saveDiagnostics(
            WebDriver driver,
            Path diagnosticDirectory,
            String reason,
            String finalUrl,
            String pageTitle,
            HabrCareerBrowserSessionProbeResultDto.Status status
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

            BrowserSessionDiagnostic diagnostic = new BrowserSessionDiagnostic(
                    normalize(reason),
                    PROFILE_URL,
                    normalize(finalUrl),
                    normalize(pageTitle),
                    status == null ? "FAILED" : status.name(),
                    Instant.now()
            );

            Files.writeString(
                    diagnosticDirectory.resolve(filePrefix + ".json"),
                    objectMapper.writeValueAsString(diagnostic),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save Habr Career browser diagnostics: "
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
                "session-probe",
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

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new HabrCareerBrowserSessionProbeException(
                    "Habr Career browser session probe was interrupted",
                    exception
            );
        }
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

    private record BrowserSessionDiagnostic(
            String reason,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            String status,
            Instant capturedAt
    ) {
    }
}
