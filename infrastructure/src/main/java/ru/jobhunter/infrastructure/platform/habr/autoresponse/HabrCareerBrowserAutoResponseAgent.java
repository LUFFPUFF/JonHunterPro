package ru.jobhunter.infrastructure.platform.habr.autoresponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.infrastructure.platform.habr.browser.HabrCareerBrowserDriverFactory;
import ru.jobhunter.infrastructure.platform.habr.browser.HabrCareerBrowserSessionProperties;
import ru.jobhunter.infrastructure.platform.habr.browser.HabrCareerResponseFormExtractor;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public final class HabrCareerBrowserAutoResponseAgent {

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerBrowserAutoResponseAgent.class
    );
    private static final String HABR_CAREER_HOST = "career.habr.com";
    private static final String VACANCY_ROOT_SELECTOR = "article.vacancy-show";

    private final HabrCareerBrowserDriverFactory driverFactory;
    private final HabrCareerBrowserSessionProperties sessionProperties;
    private final HabrCareerBrowserAutoResponseProperties properties;
    private final HabrCareerResponseFormExtractor responseFormExtractor;
    private final ObjectMapper objectMapper;

    public HabrCareerBrowserAutoResponseAgent(
            HabrCareerBrowserDriverFactory driverFactory,
            HabrCareerBrowserSessionProperties sessionProperties,
            HabrCareerBrowserAutoResponseProperties properties,
            HabrCareerResponseFormExtractor responseFormExtractor,
            ObjectMapper objectMapper
    ) {
        this.driverFactory = Objects.requireNonNull(
                driverFactory,
                "Habr Career browser driver factory must not be null"
        );
        this.sessionProperties = Objects.requireNonNull(
                sessionProperties,
                "Habr Career browser session properties must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career browser auto response properties must not be null"
        );
        this.responseFormExtractor = Objects.requireNonNull(
                responseFormExtractor,
                "Habr Career response form extractor must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
    }

    public HabrCareerBrowserAutoResponseResult apply(
            UserId userId,
            String externalVacancyId,
            String vacancyUrl,
            String coverLetter,
            UUID executionId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        String requestedExternalVacancyId = requireNotBlank(
                externalVacancyId,
                "Habr Career external vacancy id must not be blank"
        );
        String targetUrl = requireNotBlank(
                vacancyUrl,
                "Habr Career vacancy URL must not be blank"
        );
        String requestedCoverLetter = requireNotBlank(
                coverLetter,
                "Habr Career cover letter must not be blank"
        );
        Objects.requireNonNull(executionId, "Execution id must not be null");

        Path diagnosticDirectory = diagnosticDirectory(userId, executionId);
        WebDriver driver = null;
        BrowserState state = new BrowserState(
                requestedExternalVacancyId,
                targetUrl,
                requestedCoverLetter.length()
        );

        try {
            driver = driverFactory.createDriver();
            driver.get(targetUrl);
            waitUntilDocumentIsReady(driver);
            state.finalUrl = normalize(driver.getCurrentUrl());
            state.pageTitle = normalize(driver.getTitle());

            if (isAuthenticationPage(state.finalUrl)) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "Требуется авторизация в Habr Career browser profile."
                );
            }

            if (!isExpectedVacancyUrl(
                    state.finalUrl,
                    requestedExternalVacancyId
            )) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "Habr Career открыл непредвиденную страницу вместо вакансии."
                );
            }

            state.vacancyRootPresent = waitForVacancyRoot(driver);
            if (!state.vacancyRootPresent) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "На странице Habr Career не найден корневой блок вакансии."
                );
            }

            state.responseKind = responseFormExtractor.responseActionKind(driver);
            state.responseAlreadyExists = responseFormExtractor
                    .hasExistingEditableResponse(driver);

            if (state.responseAlreadyExists) {
                if (properties.mode() == HabrCareerBrowserAutoResponseMode.PREFLIGHT) {
                    return completed(
                            HabrCareerBrowserAutoResponseOutcome.PREFLIGHT_VERIFIED,
                            state,
                            diagnosticDirectory,
                            "Pre-flight завершён: по вакансии уже существует "
                                    + "редактируемый отклик Habr Career."
                    );
                }

                return attachCoverLetterToExistingResponse(
                        driver,
                        state,
                        requestedCoverLetter,
                        diagnosticDirectory
                );
            }

            Optional<WebElement> initialResponseAction = responseFormExtractor
                    .findInitialResponseAction(driver);

            if (initialResponseAction.isEmpty()) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "Не найдена точная доступная кнопка «Откликнуться»."
                );
            }

            state.initialResponseActionPresent = true;

            if (!responseFormExtractor.isDirectResponseAction(driver)) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "Тип действия отклика Habr Career не подтверждён как direct."
                );
            }

            state.directResponseConfirmed = true;

            if (properties.mode() == HabrCareerBrowserAutoResponseMode.PREFLIGHT) {
                return completed(
                        HabrCareerBrowserAutoResponseOutcome.PREFLIGHT_VERIFIED,
                        state,
                        diagnosticDirectory,
                        "Pre-flight завершён: Habr Career готов к direct-отклику, "
                                + "но режим EXECUTE не включён."
                );
            }

            click(driver, initialResponseAction.get());
            state.initialResponseActionClicked = true;

            if (!waitForExistingEditableResponse(driver)) {
                driver.navigate().refresh();
                waitUntilDocumentIsReady(driver);
                state.finalUrl = normalize(driver.getCurrentUrl());
                state.pageTitle = normalize(driver.getTitle());
            }

            if (!responseFormExtractor.hasExistingEditableResponse(driver)) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "Первичная кнопка direct-отклика была нажата, но новый "
                                + "отклик не удалось надёжно подтвердить. "
                                + "Повторная отправка заблокирована."
                );
            }

            state.responseAlreadyExists = true;
            return attachCoverLetterToExistingResponse(
                    driver,
                    state,
                    requestedCoverLetter,
                    diagnosticDirectory
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Habr Career browser execution failed: externalVacancyId={}, "
                            + "directClicked={}",
                    requestedExternalVacancyId,
                    state.initialResponseActionClicked,
                    exception
            );

            if (state.initialResponseActionClicked) {
                return candidateApproval(
                        state,
                        diagnosticDirectory,
                        "Состояние после нажатия direct-отклика не подтверждено: "
                                + rootMessage(exception)
                );
            }

            throw new HabrCareerAutoResponseExecutionException(
                    "Habr Career browser execution failed: "
                            + rootMessage(exception),
                    exception
            );
        } finally {
            saveDiagnostic(state, diagnosticDirectory);
            closeDriver(driver, executionId);
        }
    }

    private HabrCareerBrowserAutoResponseResult
    attachCoverLetterToExistingResponse(
            WebDriver driver,
            BrowserState state,
            String requestedCoverLetter,
            Path diagnosticDirectory
    ) {
        Optional<WebElement> editAction = responseFormExtractor
                .findExistingResponseEditAction(driver);

        if (editAction.isEmpty()) {
            return candidateApproval(
                    state,
                    diagnosticDirectory,
                    "Отклик существует, но точная кнопка «Редактировать» не найдена."
            );
        }

        click(driver, editAction.get());
        state.editActionClicked = true;

        Optional<WebElement> coverLetterInput = waitForCoverLetterInput(driver);
        if (coverLetterInput.isEmpty()) {
            return partialSuccess(
                    state,
                    diagnosticDirectory,
                    "Habr Career принял отклик, но поле сопроводительного письма "
                            + "в форме редактирования не найдено."
            );
        }

        state.coverLetterInputFound = true;
        WebElement input = coverLetterInput.get();
        String existingCoverLetter = responseFormExtractor.readInputValue(
                driver,
                input
        );

        if (!existingCoverLetter.isBlank()) {
            state.existingCoverLetterPresent = true;

            if (normalizeContent(existingCoverLetter).equals(
                    normalizeContent(requestedCoverLetter)
            )) {
                state.coverLetterVerified = true;
                return completed(
                        HabrCareerBrowserAutoResponseOutcome
                                .RESPONSE_SENT_WITH_COVER_LETTER,
                        state,
                        diagnosticDirectory,
                        "Отклик Habr Career уже содержит подтверждённое "
                                + "сопроводительное письмо."
                );
            }

            return completed(
                    HabrCareerBrowserAutoResponseOutcome.ALREADY_RESPONDED,
                    state,
                    diagnosticDirectory,
                    "По вакансии уже существует отклик Habr Career с "
                            + "сопроводительным письмом. Перезапись запрещена."
            );
        }

        int maximumLength = responseFormExtractor.inputMaxLength(input);
        state.coverLetterMaximumLength = maximumLength;
        if (maximumLength > 0 && requestedCoverLetter.length() > maximumLength) {
            return partialSuccess(
                    state,
                    diagnosticDirectory,
                    "Habr Career принял отклик, но письмо превышает ограничение "
                            + "формы: " + maximumLength + " символов."
            );
        }

        input.clear();
        input.sendKeys(requestedCoverLetter);
        String enteredCoverLetter = responseFormExtractor.readInputValue(
                driver,
                input
        );

        if (!normalizeContent(enteredCoverLetter).equals(
                normalizeContent(requestedCoverLetter)
        )) {
            return partialSuccess(
                    state,
                    diagnosticDirectory,
                    "Habr Career принял отклик, но текст письма не удалось "
                            + "надёжно внести в форму."
            );
        }

        Optional<WebElement> saveAction = responseFormExtractor
                .findExistingResponseEditSaveAction(driver);
        if (saveAction.isEmpty()) {
            return partialSuccess(
                    state,
                    diagnosticDirectory,
                    "Habr Career принял отклик, но кнопка «Сохранить» "
                            + "не найдена."
            );
        }

        click(driver, saveAction.get());
        state.saveActionClicked = true;

        if (!verifySavedCoverLetter(driver, requestedCoverLetter)) {
            return partialSuccess(
                    state,
                    diagnosticDirectory,
                    "Habr Career принял отклик, но сохранение письма "
                            + "не подтверждено. Повторная отправка не выполнялась."
            );
        }

        state.coverLetterVerified = true;
        return completed(
                HabrCareerBrowserAutoResponseOutcome
                        .RESPONSE_SENT_WITH_COVER_LETTER,
                state,
                diagnosticDirectory,
                "Отклик Habr Career отправлен, сопроводительное письмо "
                        + "сохранено и подтверждено."
        );
    }

    private boolean verifySavedCoverLetter(
            WebDriver driver,
            String requestedCoverLetter
    ) {
        try {
            driver.navigate().refresh();
            waitUntilDocumentIsReady(driver);

            if (!waitForExistingEditableResponse(driver)) {
                return false;
            }

            Optional<WebElement> editAction = responseFormExtractor
                    .findExistingResponseEditAction(driver);
            if (editAction.isEmpty()) {
                return false;
            }

            click(driver, editAction.get());
            Optional<WebElement> input = waitForCoverLetterInput(driver);
            if (input.isEmpty()) {
                return false;
            }

            String actual = responseFormExtractor.readInputValue(
                    driver,
                    input.get()
            );
            return normalizeContent(actual).equals(
                    normalizeContent(requestedCoverLetter)
            );
        } catch (RuntimeException exception) {
            log.warn("Could not verify saved Habr Career cover letter", exception);
            return false;
        }
    }

    private boolean waitForExistingEditableResponse(WebDriver driver) {
        try {
            return new WebDriverWait(driver, sessionProperties.waitTimeout())
                    .until(responseFormExtractor::hasExistingEditableResponse);
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private Optional<WebElement> waitForCoverLetterInput(WebDriver driver) {
        try {
            WebElement input = new WebDriverWait(
                    driver,
                    sessionProperties.waitTimeout()
            ).until(webDriver -> responseFormExtractor
                    .findExistingResponseCoverLetterInput(webDriver)
                    .orElse(null));
            return Optional.ofNullable(input);
        } catch (TimeoutException exception) {
            return Optional.empty();
        }
    }

    private boolean waitForVacancyRoot(WebDriver driver) {
        try {
            return new WebDriverWait(driver, sessionProperties.waitTimeout())
                    .until(webDriver -> !webDriver.findElements(
                                    By.cssSelector(
                                            VACANCY_ROOT_SELECTOR
                                    )
                            )
                            .isEmpty());
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private void waitUntilDocumentIsReady(WebDriver driver) {
        new WebDriverWait(driver, sessionProperties.waitTimeout())
                .until(webDriver -> "complete".equals(
                        ((JavascriptExecutor) webDriver).executeScript(
                                "return document.readyState"
                        )
                ));
    }

    private HabrCareerBrowserAutoResponseResult candidateApproval(
            BrowserState state,
            Path diagnosticDirectory,
            String reason
    ) {
        state.candidateApprovalReason = reason;
        return completed(
                HabrCareerBrowserAutoResponseOutcome.CANDIDATE_APPROVAL_REQUIRED,
                state,
                diagnosticDirectory,
                "Отклик остановлен: требуется подтверждение кандидата."
        );
    }

    private HabrCareerBrowserAutoResponseResult partialSuccess(
            BrowserState state,
            Path diagnosticDirectory,
            String message
    ) {
        return completed(
                HabrCareerBrowserAutoResponseOutcome.RESPONSE_SENT_WITHOUT_COVER_LETTER,
                state,
                diagnosticDirectory,
                message
        );
    }

    private HabrCareerBrowserAutoResponseResult completed(
            HabrCareerBrowserAutoResponseOutcome outcome,
            BrowserState state,
            Path diagnosticDirectory,
            String message
    ) {
        state.outcome = outcome;
        state.message = message;
        return new HabrCareerBrowserAutoResponseResult(
                outcome,
                message,
                state.candidateApprovalReason,
                diagnosticDirectory.toString()
        );
    }

    private void saveDiagnostic(BrowserState state, Path diagnosticDirectory) {
        try {
            Files.createDirectories(diagnosticDirectory);
            Files.writeString(
                    diagnosticDirectory.resolve("execution-state.json"),
                    objectMapper.writeValueAsString(new BrowserExecutionDiagnostic(
                            state.requestedExternalVacancyId,
                            state.targetUrl,
                            state.finalUrl,
                            state.pageTitle,
                            state.vacancyRootPresent,
                            state.responseKind,
                            state.initialResponseActionPresent,
                            state.directResponseConfirmed,
                            state.initialResponseActionClicked,
                            state.responseAlreadyExists,
                            state.editActionClicked,
                            state.coverLetterInputFound,
                            state.existingCoverLetterPresent,
                            state.requestedCoverLetterLength,
                            state.coverLetterMaximumLength,
                            state.saveActionClicked,
                            state.coverLetterVerified,
                            state.outcome == null ? "FAILED" : state.outcome.name(),
                            state.message,
                            state.candidateApprovalReason,
                            Instant.now()
                    )),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save Habr Career auto-response diagnostics: {}",
                    diagnosticDirectory,
                    exception
            );
        }
    }

    private Path diagnosticDirectory(UserId userId, UUID executionId) {
        return Path.of(
                "logs",
                "habr-browser-debug",
                "auto-response",
                safePathSegment(userId.toString()),
                executionId.toString()
        ).toAbsolutePath().normalize();
    }

    private void closeDriver(WebDriver driver, UUID executionId) {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (RuntimeException exception) {
            log.warn(
                    "Habr Career browser driver could not be closed: executionId={}",
                    executionId,
                    exception
            );
        }
    }

    private static boolean isExpectedVacancyUrl(
            String finalUrl,
            String expectedExternalVacancyId
    ) {
        try {
            URI uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Browser final URL must not be blank"
            ));
            return HABR_CAREER_HOST.equalsIgnoreCase(uri.getHost())
                    && ("/vacancies/" + expectedExternalVacancyId).equals(
                    normalize(uri.getPath())
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isAuthenticationPage(String url) {
        try {
            URI uri = URI.create(requireNotBlank(url, "Browser final URL must not be blank"));
            String host = normalize(uri.getHost()).toLowerCase(Locale.ROOT);
            String path = normalize(uri.getPath()).toLowerCase(Locale.ROOT);
            return (host.endsWith(".habr.com") && !HABR_CAREER_HOST.equals(host))
                    || (HABR_CAREER_HOST.equals(host)
                    && (path.contains("/login")
                    || path.contains("/sign_in")
                    || path.contains("/sign-in")
                    || path.contains("/auth/")));
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    private static void click(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                element
        );
        element.click();
    }

    private static String requireNotBlank(String value, String message) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeContent(String value) {
        return normalize(value).replace("\r\n", "\n");
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

    private static final class BrowserState {
        private final String requestedExternalVacancyId;
        private final String targetUrl;
        private final int requestedCoverLetterLength;
        private String finalUrl = "";
        private String pageTitle = "";
        private boolean vacancyRootPresent;
        private String responseKind = "";
        private boolean initialResponseActionPresent;
        private boolean directResponseConfirmed;
        private boolean initialResponseActionClicked;
        private boolean responseAlreadyExists;
        private boolean editActionClicked;
        private boolean coverLetterInputFound;
        private boolean existingCoverLetterPresent;
        private int coverLetterMaximumLength;
        private boolean saveActionClicked;
        private boolean coverLetterVerified;
        private HabrCareerBrowserAutoResponseOutcome outcome;
        private String message;
        private String candidateApprovalReason;

        private BrowserState(
                String requestedExternalVacancyId,
                String targetUrl,
                int requestedCoverLetterLength
        ) {
            this.requestedExternalVacancyId = requestedExternalVacancyId;
            this.targetUrl = targetUrl;
            this.requestedCoverLetterLength = requestedCoverLetterLength;
        }
    }

    private record BrowserExecutionDiagnostic(
            String requestedExternalVacancyId,
            String targetUrl,
            String finalUrl,
            String pageTitle,
            boolean vacancyRootPresent,
            String responseKind,
            boolean initialResponseActionPresent,
            boolean directResponseConfirmed,
            boolean initialResponseActionClicked,
            boolean responseAlreadyExists,
            boolean editActionClicked,
            boolean coverLetterInputFound,
            boolean existingCoverLetterPresent,
            int requestedCoverLetterLength,
            int coverLetterMaximumLength,
            boolean saveActionClicked,
            boolean coverLetterVerified,
            String outcome,
            String message,
            String candidateApprovalReason,
            Instant capturedAt
    ) {
    }
}
