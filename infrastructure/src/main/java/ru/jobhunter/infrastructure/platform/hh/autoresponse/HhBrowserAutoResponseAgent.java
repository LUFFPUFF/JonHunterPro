package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswerDto;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswersDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireAnswerQuality;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;
import ru.jobhunter.core.application.exception.QuestionnaireAnswerGenerationUnavailableException;
import ru.jobhunter.core.application.usecase.questionnaire.GenerateHhQuestionnaireAnswersUseCase;
import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireFieldType;
import ru.jobhunter.core.application.usecase.questionnaire.GenerateHhQuestionnaireChoiceAnswersUseCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionException;


@Component
public class HhBrowserAutoResponseAgent {

    private static final Logger log =
            LoggerFactory.getLogger(HhBrowserAutoResponseAgent.class);

    private static final String RESPONSE_SENT_VISIBLE_TEXT =
            "Вы откликнулись";

    private static final By RESPONSE_SENT_CHAT_BUTTON_SELECTOR =
            By.cssSelector("[data-qa='vacancy-response-link-view-topic']");

    private static final List<By> RESPONSE_BUTTON_SELECTORS = List.of(
            By.cssSelector("[data-qa='vacancy-response-link-top']"),
            By.cssSelector("[data-qa='vacancy-response-link-bottom']"),
            By.cssSelector("[data-qa='vacancy-response-button-top']"),
            By.cssSelector("[data-qa='vacancy-response-button-bottom']"),
            By.xpath("//a[contains(normalize-space(.), 'Откликнуться')]"),
            By.xpath("//button[contains(normalize-space(.), 'Откликнуться')]")
    );

    private static final List<By> ADD_COVER_LETTER_SELECTORS = List.of(
            By.cssSelector("[data-qa='vacancy-response-letter-toggle']"),
            By.cssSelector("[data-qa='add-cover-letter']"),
            By.cssSelector("[data-qa*='cover-letter']"),
            By.xpath("//button[contains(normalize-space(.), 'сопровод')]"),
            By.xpath("//a[contains(normalize-space(.), 'сопровод')]"),
            By.xpath("//span[contains(normalize-space(.), 'сопровод')]/ancestor::button"),
            By.xpath("//span[contains(normalize-space(.), 'сопровод')]/ancestor::a")
    );

    private static final List<By> TEXTAREA_SELECTORS = List.of(
            By.cssSelector("textarea[name='message']"),
            By.cssSelector("textarea[name='letter']"),
            By.cssSelector("textarea[data-qa='vacancy-response-popup-form-letter-input']"),
            By.cssSelector("textarea[data-qa*='letter']"),
            By.cssSelector("textarea[data-qa*='message']"),
            By.xpath("//textarea")
    );

    private static final List<By>
            POST_RESPONSE_ATTACH_COVER_LETTER_SELECTORS = List.of(
            By.cssSelector(
                    "[data-qa='responded-success-attach-cover-letter']"
            ),
            By.xpath(
                    "//*[@data-qa='responded-success-attach-cover-letter']"
            ),
            By.xpath(
                    "//button[normalize-space(.)="
                            + "'Приложить сопроводительное письмо']"
            ),
            By.xpath(
                    "//a[normalize-space(.)="
                            + "'Приложить сопроводительное письмо']"
            )
    );

    private static final List<By>
            POST_RESPONSE_LETTER_TEXTAREA_SELECTORS = List.of(
            By.cssSelector(
                    "[data-qa='modal-overlay'] "
                            + "form[id^='cover-letter-ai-'] "
                            + "textarea[name='text']"
            ),
            By.cssSelector(
                    "[role='dialog'] "
                            + "form[id^='cover-letter-ai-'] "
                            + "textarea[name='text']"
            )
    );

    private static final List<By>
            POST_RESPONSE_LETTER_SUBMIT_SELECTORS = List.of(
            By.cssSelector(
                    "[data-qa='modal-overlay'] "
                            + "[data-qa='vacancy-response-letter-submit']"
            ),
            By.cssSelector(
                    "[role='dialog'] "
                            + "[data-qa='vacancy-response-letter-submit']"
            )
    );

    private static final List<By>
            POST_RESPONSE_LETTER_SUCCESS_SELECTORS = List.of(
            By.xpath(
                    "//*[@data-qa='vacancy-response-letter-informer']"
                            + "[contains(normalize-space(.), "
                            + "'Сопроводительное письмо отправлено')]"
            )
    );

    private static final List<By> SUBMIT_BUTTON_SELECTORS = List.of(
            By.cssSelector("[data-qa='vacancy-response-submit-popup']"),
            By.cssSelector("[data-qa='vacancy-response-submit']"),
            By.cssSelector("button[type='submit']"),
            By.xpath("//button[contains(normalize-space(.), 'Отправить')]"),
            By.xpath("//button[contains(normalize-space(.), 'Откликнуться')]")
    );

    private static final List<By> QUESTIONNAIRE_SUBMIT_BUTTON_SELECTORS = List.of(
            By.cssSelector("[data-qa='task-submit']"),
            By.cssSelector("[data-qa*='task'][data-qa*='submit']"),
            By.xpath(
                    "//textarea[starts-with(@name, 'task_')]"
                            + "/ancestor::form[1]//button[@type='submit']"
            ),
            By.xpath(
                    "//button[contains(normalize-space(.), 'Отправить')]"
            ),
            By.xpath(
                    "//button[contains(normalize-space(.), 'Откликнуться')]"
            )
    );

    private static final List<By> QUESTION_FORM_MARKERS = List.of(
            By.xpath("//*[contains(normalize-space(.), 'Ответьте на вопросы')]"),
            By.xpath("//*[contains(normalize-space(.), 'ответьте на вопросы')]"),
            By.xpath("//*[contains(normalize-space(.), 'Вопросы работодателя')]"),
            By.xpath("//*[contains(normalize-space(.), 'Работодатель просит ответить')]"),
            By.cssSelector("input[type='radio']"),
            By.cssSelector("input[type='checkbox']")
    );

    private static final By QUESTIONNAIRE_TASK_BODY_SELECTOR = By.cssSelector("[data-qa='task-body']");

    private static final By QUESTIONNAIRE_QUESTION_TEXT_SELECTOR = By.cssSelector("[data-qa='task-question']");

    private static final By QUESTIONNAIRE_TEXTAREA_SELECTOR = By.cssSelector("textarea[name^='task_'][name$='_text']");

    private final HhBrowserDriverFactory driverFactory;
    private final HhBrowserAutoResponseProperties properties;
    private final HhQuestionnaireExecutionProperties questionnaireExecutionProperties;
    private final GenerateHhQuestionnaireAnswersUseCase generateHhQuestionnaireAnswersUseCase;
    private final ObjectMapper objectMapper;

    public HhBrowserAutoResponseAgent(
            HhBrowserDriverFactory driverFactory,
            HhBrowserAutoResponseProperties properties,
            HhQuestionnaireExecutionProperties questionnaireExecutionProperties,
            GenerateHhQuestionnaireAnswersUseCase generateHhQuestionnaireAnswersUseCase,
            ObjectMapper objectMapper
    ) {
        this.driverFactory = Objects.requireNonNull(
                driverFactory,
                "HH browser driver factory must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "HH browser auto response properties must not be null"
        );
        this.questionnaireExecutionProperties = Objects.requireNonNull(
                questionnaireExecutionProperties,
                "HH questionnaire execution properties must not be null"
        );
        this.generateHhQuestionnaireAnswersUseCase = Objects.requireNonNull(
                generateHhQuestionnaireAnswersUseCase,
                "Generate HH questionnaire text answers use case "
                        + "must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
    }

    public HhBrowserAutoResponseOutcome apply(
            String vacancyUrl,
            String coverLetter
    ) {
        return applyDetailed(
                "unknown",
                vacancyUrl,
                coverLetter,
                null,
                UUID.randomUUID()
        ).outcome();
    }

    public HhBrowserAutoResponseOutcome apply(
            String vacancyUrl,
            String coverLetter,
            UUID executionId
    ) {
        return applyDetailed(
                "unknown",
                vacancyUrl,
                coverLetter,
                null,
                executionId
        ).outcome();
    }

    public HhBrowserAutoResponseOutcome apply(
            String vacancyUrl,
            String coverLetter,
            HhQuestionnaireGenerationContext questionnaireContext,
            UUID executionId
    ) {
        return applyDetailed(
                "unknown",
                vacancyUrl,
                coverLetter,
                questionnaireContext,
                executionId
        ).outcome();
    }

    public HhBrowserAutoResponseResult applyDetailed(
            String externalVacancyId,
            String vacancyUrl,
            String coverLetter,
            HhQuestionnaireGenerationContext questionnaireContext,
            UUID executionId
    ) {
        String normalizedExternalVacancyId = requireNotBlank(
                externalVacancyId,
                "HH external vacancy id must not be blank"
        );

        String normalizedVacancyUrl = requireNotBlank(
                vacancyUrl,
                "HH vacancy URL must not be blank"
        );

        String normalizedCoverLetter = requireNotBlank(
                coverLetter,
                "HH cover letter must not be blank"
        );

        Objects.requireNonNull(
                executionId,
                "HH browser execution id must not be null"
        );

        WebDriver driver = driverFactory.createDriver();

        BrowserRunContext context = new BrowserRunContext(
                executionId,
                normalizedExternalVacancyId,
                browserSessionId(driver)
        );

        log.info("HH browser run started: executionId={}, sessionId={}, vacancyUrl={}", context.executionId(), context.browserSessionId(), normalizedVacancyUrl);
        try {
            WebDriverWait wait = new WebDriverWait(driver, properties.waitTimeout());
            driver.get(normalizedVacancyUrl);
            waitUntilPageLoaded(driver, wait);
            ensureLoggedIn(driver);
            Optional<WebElement> responseButton = findFirstVisibleNow(driver, RESPONSE_BUTTON_SELECTORS);
            if (responseButton.isPresent()) {
                WebElement button = responseButton.get();
                if (properties.mode().isPreflight()) {
                    scrollIntoView(driver, button);
                    saveDiagnosticsOnce(driver, context, "preflight-response-button-verified");
                    return toBrowserResult(
                            HhBrowserAutoResponseOutcome.PREFLIGHT_VERIFIED,
                            context
                    );
                }
                scrollIntoView(driver, button);
                clickWithJavaScript(driver, button);
                sleepSilently(1200);
                waitUntilPageLoaded(driver, wait);
                ensureLoggedIn(driver);
            } else if (isResponseAlreadySent(driver)) {
                saveDiagnosticsOnce(driver, context, "already-responded");
                return toBrowserResult(
                        HhBrowserAutoResponseOutcome.ALREADY_RESPONDED,
                        context
                );
            } else {
                saveDiagnosticsOnce(driver, context, "response-state-uncertain");
                throw new HhAutoResponseExecutionException("HH vacancy response state is uncertain: " + "the response button and confirmed success indicator " + "were not found. Current URL: " + driver.getCurrentUrl());
            }
            if (isResponseAlreadySent(driver)) {
                return toBrowserResult(
                        tryAttachPostResponseCoverLetter(
                                driver,
                                normalizedCoverLetter,
                                context
                        ),
                        context
                );
            }
            if (hasQuestionForm(driver)) {
                return toBrowserResult(
                        fillQuestionnaireForReview(
                                driver,
                                normalizedCoverLetter,
                                questionnaireContext,
                                context
                        ),
                        context
                );
            }
            findFirstVisibleNow(driver, ADD_COVER_LETTER_SELECTORS)
                    .ifPresent(addLetterButton -> {
                        scrollIntoView(driver, addLetterButton);
                        clickWithJavaScript(driver, addLetterButton);
                        sleepSilently(700);
                    });

            WebElement textarea = findFirstVisibleWithDeadline(
                    driver,
                    TEXTAREA_SELECTORS,
                    properties.waitTimeout()
            ).orElseThrow(() -> {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "cover-letter-form-not-found"
                );

                return new HhAutoResponseExecutionException(
                        "HH cover letter textarea was not found. "
                                + "Strict policy prohibited response submission. "
                                + "Current URL: "
                                + driver.getCurrentUrl()
                );
            });

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "cover-letter-form-opened"
            );

            fillCoverLetterTextareaAndVerify(
                    driver,
                    textarea,
                    normalizedCoverLetter,
                    context
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "cover-letter-dom-verified"
            );

            WebElement submitButton = findFirstVisibleWithDeadline(
                    driver,
                    SUBMIT_BUTTON_SELECTORS,
                    properties.waitTimeout()
            ).orElseThrow(() -> {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "cover-letter-submit-button-not-found"
                );

                return new HhAutoResponseExecutionException(
                        "HH response submit button was not found after "
                                + "cover letter DOM verification. "
                                + "Current URL: "
                                + driver.getCurrentUrl()
                );
            });

            scrollIntoView(driver, submitButton);

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "cover-letter-submit-ready"
            );

            clickWithJavaScript(driver, submitButton);

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "cover-letter-submit-clicked"
            );

            return toBrowserResult(
                    waitForStateAfterCoverLetterSubmit(
                            driver,
                            normalizedCoverLetter,
                            questionnaireContext,
                            context
                    ),
                    context
            );
        } catch (RuntimeException exception) {
            saveDiagnosticsOnce(driver, context, "execution-error");
            throw exception;
        } finally {
            log.info("HH browser run finished: executionId={}, sessionId={}", context.executionId(), context.browserSessionId());
            try {
                driver.quit();
            } catch (RuntimeException exception) {
                log.warn("HH browser driver could not be closed: " + "executionId={}, sessionId={}", context.executionId(), context.browserSessionId(), exception);
            }
        }
    }

    private HhBrowserAutoResponseResult toBrowserResult(
            HhBrowserAutoResponseOutcome outcome,
            BrowserRunContext context
    ) {
        if (context.candidateApprovalReason() != null) {
            return HhBrowserAutoResponseResult.candidateApprovalRequired(
                    context.candidateApprovalReason(),
                    context.diagnosticsRelativeDirectory()
            );
        }

        return HhBrowserAutoResponseResult.completed(
                outcome,
                context.diagnosticsRelativeDirectory()
        );
    }

    private boolean isResponseAlreadySent(WebDriver driver) {
        if (findFirstVisibleNow(
                driver,
                List.of(
                        By.cssSelector(
                                "[data-qa="
                                        + "'vacancy-response-success-standard-"
                                        + "notification']"
                        )
                )
        ).isPresent()) {
            return true;
        }

        return containsVisiblePageText(
                driver,
                RESPONSE_SENT_VISIBLE_TEXT
        ) && findFirstVisibleNow(
                driver,
                List.of(RESPONSE_SENT_CHAT_BUTTON_SELECTOR)
        ).isPresent();
    }

    private boolean containsVisiblePageText(
            WebDriver driver,
            String expectedText
    ) {
        Object rawText = ((JavascriptExecutor) driver).executeScript(
                "return document.body ? document.body.innerText : '';"
        );

        if (!(rawText instanceof String pageText)) {
            return false;
        }

        return normalizeVisibleText(pageText).contains(
                normalizeVisibleText(expectedText)
        );
    }

    private String normalizeVisibleText(String value) {
        return value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void saveDiagnosticsOnce(
            WebDriver driver,
            BrowserRunContext context,
            String reason
    ) {
        if (!context.markDiagnosticsSaved(reason)) {
            log.debug(
                    "HH browser diagnostics skipped because this reason was "
                            + "already saved: executionId={}, sessionId={}, reason={}",
                    context.executionId(),
                    context.browserSessionId(),
                    reason
            );
            return;
        }

        try {
            Path runDirectory = Path.of(
                    context.diagnosticsRelativeDirectory()
            ).toAbsolutePath().normalize();

            Files.createDirectories(runDirectory);

            String safeTimestamp = Instant.now()
                    .toString()
                    .replace(":", "-")
                    .replace(".", "-");

            String safeReason = safePathSegment(reason);

            String safeSessionId = safePathSegment(
                    context.browserSessionId()
            );

            String filePrefix = safeReason
                    + "-"
                    + safeTimestamp
                    + "-session-"
                    + safeSessionId;

            Path htmlPath = runDirectory.resolve(
                    filePrefix + ".html"
            );

            Files.writeString(
                    htmlPath,
                    Objects.requireNonNull(driver.getPageSource()),
                    StandardCharsets.UTF_8
            );

            Path screenshotPath = runDirectory.resolve(
                    filePrefix + ".png"
            );

            boolean screenshotSaved = false;

            if (driver instanceof TakesScreenshot screenshotDriver) {
                FileHandler.copy(
                        screenshotDriver.getScreenshotAs(OutputType.FILE),
                        screenshotPath.toFile()
                );

                screenshotSaved = true;
            }

            log.info(
                    "HH browser diagnostics saved: executionId={}, sessionId={}, "
                            + "reason={}, htmlPath={}, screenshotSaved={}",
                    context.executionId(),
                    context.browserSessionId(),
                    reason,
                    htmlPath,
                    screenshotSaved
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save HH browser diagnostics: "
                            + "executionId={}, sessionId={}, reason={}",
                    context.executionId(),
                    context.browserSessionId(),
                    reason,
                    exception
            );
        }
    }

    private String browserSessionId(WebDriver driver) {
        if (driver instanceof RemoteWebDriver remoteWebDriver) {
            Object sessionId = remoteWebDriver.getSessionId();
            if (sessionId != null) {
                return sessionId.toString();
            }
        }
        return "unknown";
    }

    private static String safePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }

    private HhBrowserAutoResponseOutcome waitForStateAfterCoverLetterSubmit(
            WebDriver driver,
            String coverLetter,
            HhQuestionnaireGenerationContext questionnaireContext,
            BrowserRunContext context
    ) {
        long deadline = System.nanoTime()
                + properties.waitTimeout().toNanos();

        while (System.nanoTime() < deadline) {
            if (hasQuestionForm(driver)) {
                return fillQuestionnaireForReview(
                        driver,
                        coverLetter,
                        questionnaireContext,
                        context
                );
            }

            if (isResponseAlreadySent(driver)) {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "response-sent-with-cover-letter"
                );

                return HhBrowserAutoResponseOutcome
                        .RESPONSE_SENT_WITH_COVER_LETTER;
            }

            sleepSilently(250);
        }

        throw new HhAutoResponseExecutionException(
                "HH cover letter was submitted, but neither a questionnaire "
                        + "nor the response success state appeared. Current URL: "
                        + driver.getCurrentUrl()
        );
    }

    private Optional<String> findCandidateApprovalReason(
            List<GeneratedHhQuestionnaireAnswerDto> answers
    ) {
        return answers.stream()
                .filter(answer ->
                        answer.quality()
                                .requiresCandidateApproval()
                )
                .map(GeneratedHhQuestionnaireAnswerDto::reviewReason)
                .filter(reason ->
                        reason != null && !reason.isBlank()
                )
                .map(String::trim)
                .findFirst();
    }

    private HhBrowserAutoResponseOutcome fillQuestionnaireForReview(
            WebDriver driver,
            String coverLetter,
            HhQuestionnaireGenerationContext questionnaireContext,
            BrowserRunContext context
    ) {
        if (questionnaireContext == null) {
            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-context-not-provided"
            );

            return HhBrowserAutoResponseOutcome.QUESTIONNAIRE_REQUIRED;
        }

        Optional<List<HhQuestionnaireQuestionDto>> questions =
                extractSupportedQuestionnaireQuestions(driver);

        if (questions.isEmpty()) {
            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-unsupported-field-type"
            );

            return HhBrowserAutoResponseOutcome.QUESTIONNAIRE_REQUIRED;
        }

        GeneratedHhQuestionnaireAnswersDto generatedAnswers;

        try {
            generatedAnswers = generateQuestionnaireAnswers(
                    questionnaireContext,
                    questions.get()
            );

            Optional<String> candidateApprovalReason =
                    findCandidateApprovalReason(
                            generatedAnswers.answers()
                    );

            candidateApprovalReason.ifPresent(
                    context::markCandidateApprovalRequired
            );
        } catch (CompletionException exception) {
            if (!containsQuestionnaireGenerationUnavailable(exception)) {
                throw exception;
            }

            log.warn(
                    "HH questionnaire generation is temporarily unavailable: "
                            + "executionId={}, sessionId={}",
                    context.executionId(),
                    context.browserSessionId(),
                    exception
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-generation-unavailable"
            );

            return HhBrowserAutoResponseOutcome.QUESTIONNAIRE_REQUIRED;
        }

        if (questionnaireExecutionProperties.isDiagnosticOnly()) {
            QuestionnaireFillSummary fillSummary =
                    new QuestionnaireFillSummary(
                            0,
                            0,
                            0,
                            0
                    );

            saveQuestionnaireReviewDiagnostic(
                    context,
                    questions.get(),
                    generatedAnswers,
                    fillSummary,
                    true,
                    "DIAGNOSTIC_ONLY"
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-diagnostic-only"
            );

            log.info(
                    "HH questionnaire execution stopped in diagnostic-only mode: "
                            + "executionId={}, sessionId={}, provider={}, model={}, "
                            + "answersCount={}",
                    context.executionId(),
                    context.browserSessionId(),
                    generatedAnswers.provider(),
                    generatedAnswers.model(),
                    generatedAnswers.answers().size()
            );

            return HhBrowserAutoResponseOutcome.QUESTIONNAIRE_REQUIRED;
        }

        QuestionnaireFillSummary fillSummary =
                fillQuestionnaireFields(
                        driver,
                        questions.get(),
                        generatedAnswers.answers()
                );

        saveQuestionnaireReviewDiagnostic(
                context,
                questions.get(),
                generatedAnswers,
                fillSummary,
                false,
                ""
        );

        if (fillSummary.reviewSkippedCount() > 0) {
            log.info(
                    "HH questionnaire requires confirmed candidate facts: "
                            + "executionId={}, sessionId={}, requiredFactsCount={}",
                    context.executionId(),
                    context.browserSessionId(),
                    fillSummary.reviewSkippedCount()
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-candidate-fact-required"
            );

            return HhBrowserAutoResponseOutcome.QUESTIONNAIRE_REQUIRED;
        }

        log.info(
                "HH questionnaire fields processed: "
                        + "executionId={}, sessionId={}, "
                        + "confirmedFilled={}, profileDerivedFilled={}, "
                        + "safeDefaultFilled={}, reviewSkipped={}",
                context.executionId(),
                context.browserSessionId(),
                fillSummary.confirmedFilledCount(),
                fillSummary.profileDerivedFilledCount(),
                fillSummary.safeDefaultFilledCount(),
                fillSummary.reviewSkippedCount()
        );

        return submitQuestionnaireAndWaitForResponseSent(
                driver,
                coverLetter,
                context
        );
    }

    private HhBrowserAutoResponseOutcome submitQuestionnaireAndWaitForResponseSent(
            WebDriver driver,
            String coverLetter,
            BrowserRunContext context
    ) {
        WebElement submitButton = findFirstVisibleWithDeadline(
                driver,
                QUESTIONNAIRE_SUBMIT_BUTTON_SELECTORS,
                properties.waitTimeout()
        ).orElseThrow(() -> {
            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-submit-button-not-found"
            );

            return new HhAutoResponseExecutionException(
                    "HH questionnaire submit button was not found. "
                            + "Current URL: "
                            + driver.getCurrentUrl()
            );
        });

        scrollIntoView(driver, submitButton);
        clickWithJavaScript(driver, submitButton);

        saveDiagnosticsOnce(
                driver,
                context,
                "questionnaire-submit-clicked"
        );

        waitForResponseSent(driver);

        saveDiagnosticsOnce(
                driver,
                context,
                "questionnaire-response-sent"
        );

        if (findFirstVisibleNow(
                driver,
                POST_RESPONSE_LETTER_SUCCESS_SELECTORS
        ).isPresent()) {
            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-cover-letter-already-confirmed"
            );

            return HhBrowserAutoResponseOutcome
                    .RESPONSE_SENT_WITH_COVER_LETTER;
        }

        HhBrowserAutoResponseOutcome attachmentOutcome =
                tryAttachPostResponseCoverLetter(
                        driver,
                        coverLetter,
                        context
                );

        if (attachmentOutcome
                == HhBrowserAutoResponseOutcome
                .RESPONSE_SENT_WITH_COVER_LETTER) {
            saveDiagnosticsOnce(
                    driver,
                    context,
                    "questionnaire-submitted-with-confirmed-cover-letter"
            );

            return attachmentOutcome;
        }

        saveDiagnosticsOnce(
                driver,
                context,
                "questionnaire-submitted-post-response-letter-not-confirmed"
        );

        return attachmentOutcome;
    }

    private Optional<List<HhQuestionnaireQuestionDto>>
    extractSupportedQuestionnaireQuestions(
            WebDriver driver
    ) {
        List<WebElement> taskBodies = driver.findElements(
                QUESTIONNAIRE_TASK_BODY_SELECTOR
        );

        if (taskBodies.isEmpty()) {
            return Optional.empty();
        }

        List<HhQuestionnaireQuestionDto> questions = new ArrayList<>();
        Set<String> fieldNames = new HashSet<>();

        for (WebElement taskBody : taskBodies) {
            List<WebElement> questionElements = taskBody.findElements(
                    QUESTIONNAIRE_QUESTION_TEXT_SELECTOR
            );

            List<WebElement> textareas = taskBody.findElements(
                    QUESTIONNAIRE_TEXTAREA_SELECTOR
            );

            List<WebElement> radioButtons = taskBody.findElements(
                    By.cssSelector("input[type='radio']")
            );

            boolean hasUnsupportedChoiceField = !taskBody.findElements(
                    By.cssSelector("input[type='checkbox'], select")
            ).isEmpty();

            if (questionElements.size() != 1
                    || hasUnsupportedChoiceField) {
                return Optional.empty();
            }

            String questionText = compact(
                    questionElements.getFirst().getText()
            );

            if (questionText.isBlank()) {
                return Optional.empty();
            }

            if (textareas.size() == 1 && radioButtons.isEmpty()) {
                String fieldName = compact(
                        textareas.getFirst().getDomAttribute("name")
                );

                if (!fieldName.matches("task_\\d+_text")
                        || !fieldNames.add(fieldName)) {
                    return Optional.empty();
                }

                questions.add(
                        new HhQuestionnaireQuestionDto(
                                fieldName,
                                questionText
                        )
                );

                continue;
            }

            if (radioButtons.size() < 2) {
                return Optional.empty();
            }

            String radioFieldName = compact(
                    radioButtons.getFirst().getDomAttribute("name")
            );

            if (!radioFieldName.matches("task_\\d+")) {
                return Optional.empty();
            }

            Optional<OtherTextBinding> otherTextBinding;

            if (textareas.isEmpty()) {
                otherTextBinding = Optional.empty();
            } else if (textareas.size() == 1) {
                otherTextBinding = extractOtherTextBinding(
                        driver,
                        radioFieldName,
                        radioButtons,
                        textareas.getFirst()
                );

                if (otherTextBinding.isEmpty()) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }

            Optional<HhQuestionnaireQuestionDto> radioQuestion =
                    extractRadioQuestion(
                            driver,
                            questionText,
                            radioButtons,
                            fieldNames,
                            otherTextBinding
                    );

            if (radioQuestion.isEmpty()) {
                return Optional.empty();
            }

            questions.add(radioQuestion.get());
        }

        return questions.isEmpty()
                ? Optional.empty()
                : Optional.of(List.copyOf(questions));
    }

    private Optional<HhQuestionnaireQuestionDto> extractRadioQuestion(
            WebDriver driver,
            String questionText,
            List<WebElement> radioButtons,
            Set<String> usedFieldNames,
            Optional<OtherTextBinding> otherTextBinding
    ) {
        String fieldName = compact(
                radioButtons.getFirst().getDomAttribute("name")
        );

        if (!fieldName.matches("task_\\d+")
                || !usedFieldNames.add(fieldName)) {
            return Optional.empty();
        }

        List<HhQuestionnaireOptionDto> options = new ArrayList<>();
        Set<String> optionValues = new HashSet<>();

        for (WebElement radioButton : radioButtons) {
            String currentFieldName = compact(
                    radioButton.getDomAttribute("name")
            );

            String optionValue = compact(
                    radioButton.getDomAttribute("value")
            );

            if (!fieldName.equals(currentFieldName)
                    || optionValue.isBlank()
                    || !optionValues.add(optionValue)) {
                return Optional.empty();
            }

            String optionLabel = extractRadioOptionLabel(
                    driver,
                    radioButton
            );

            if (optionLabel.isBlank()) {
                return Optional.empty();
            }

            options.add(
                    new HhQuestionnaireOptionDto(
                            optionValue,
                            optionLabel
                    )
            );
        }

        if (otherTextBinding.isPresent()) {
            OtherTextBinding binding = otherTextBinding.get();

            return Optional.of(
                    new HhQuestionnaireQuestionDto(
                            fieldName,
                            questionText,
                            HhQuestionnaireFieldType
                                    .RADIO_WITH_OTHER_TEXT,
                            options,
                            binding.otherOptionValue(),
                            binding.otherTextFieldName()
                    )
            );
        }

        return Optional.of(
                new HhQuestionnaireQuestionDto(
                        fieldName,
                        questionText,
                        HhQuestionnaireFieldType.RADIO,
                        options
                )
        );
    }

    private String extractRadioOptionLabel(
            WebDriver driver,
            WebElement radioButton
    ) {
        Object rawLabel = ((JavascriptExecutor) driver).executeScript(
                """
                const input = arguments[0];
                const label = input.closest('label');
    
                if (label && label.innerText) {
                    return label.innerText;
                }
    
                return '';
                """,
                radioButton
        );

        return rawLabel instanceof String label
                ? compact(label)
                : "";
    }

    private Optional<OtherTextBinding> extractOtherTextBinding(
            WebDriver driver,
            String radioFieldName,
            List<WebElement> radioButtons,
            WebElement textarea
    ) {
        String otherTextFieldName = compact(
                textarea.getDomAttribute("name")
        );

        if (!otherTextFieldName.equals(
                radioFieldName + "_text"
        )) {
            return Optional.empty();
        }

        Object rawOtherOptionValue =
                ((JavascriptExecutor) driver).executeScript(
                        """
                        const textarea = arguments[0];
                        const optionText = textarea.closest('.task-option-text');
    
                        if (!optionText) {
                            return '';
                        }
    
                        let sibling = optionText.previousElementSibling;
    
                        while (sibling) {
                            const radio = sibling.matches(
                                    'input[type="radio"]'
                            )
                                    ? sibling
                                    : sibling.querySelector(
                                            'input[type="radio"]'
                                    );
    
                            if (radio && radio.value) {
                                return radio.value;
                            }
    
                            sibling = sibling.previousElementSibling;
                        }
    
                        return '';
                        """,
                        textarea
                );

        String otherOptionValue = rawOtherOptionValue instanceof String value
                ? compact(value)
                : "";

        if (otherOptionValue.isBlank()) {
            return Optional.empty();
        }

        boolean radioExists = radioButtons.stream()
                .anyMatch(radio ->
                        otherOptionValue.equals(
                                compact(
                                        radio.getDomAttribute("value")
                                )
                        )
                );

        if (!radioExists) {
            return Optional.empty();
        }

        return Optional.of(
                new OtherTextBinding(
                        otherOptionValue,
                        otherTextFieldName
                )
        );
    }

    private QuestionnaireFillSummary fillQuestionnaireFields(
            WebDriver driver,
            List<HhQuestionnaireQuestionDto> questions,
            List<GeneratedHhQuestionnaireAnswerDto> answers
    ) {
        Map<String, GeneratedHhQuestionnaireAnswerDto> answersByField =
                getStringGeneratedHhQuestionnaireAnswerDtoMap(
                        questions,
                        answers
                );

        int candidateApprovalRequiredCount = 0;

        for (HhQuestionnaireQuestionDto question : questions) {
            GeneratedHhQuestionnaireAnswerDto answer =
                    answersByField.get(question.fieldName());

            if (answer == null) {
                throw new HhAutoResponseExecutionException(
                        "LLM answer was not found for field: "
                                + question.fieldName()
                );
            }

            if (answer.quality().requiresCandidateApproval()) {
                candidateApprovalRequiredCount++;
            }
        }

        if (candidateApprovalRequiredCount > 0) {
            return new QuestionnaireFillSummary(
                    0,
                    0,
                    0,
                    candidateApprovalRequiredCount
            );
        }

        int confirmedFilledCount = 0;
        int profileDerivedFilledCount = 0;
        int safeDefaultFilledCount = 0;

        for (HhQuestionnaireQuestionDto question : questions) {
            GeneratedHhQuestionnaireAnswerDto answer =
                    answersByField.get(question.fieldName());

            if (answer == null) {
                throw new HhAutoResponseExecutionException(
                        "LLM answer was not found for field: "
                                + question.fieldName()
                );
            }

            if (!answer.quality().isAutoFillAllowed()) {
                throw new HhAutoResponseExecutionException(
                        "LLM returned questionnaire answer that is not "
                                + "allowed for automatic filling: "
                                + question.fieldName()
                );
            }

            if (question.isText()) {
                fillTextQuestionnaireField(
                        driver,
                        question,
                        answer
                );
            } else if (question.isRadio()) {
                fillRadioQuestionnaireField(
                        driver,
                        question,
                        answer
                );
            } else if (question.isRadioWithOtherText()) {
                fillRadioWithOtherTextQuestionnaireField(
                        driver,
                        question,
                        answer
                );
            } else {
                throw new HhAutoResponseExecutionException(
                        "Unsupported questionnaire field type: "
                                + question.fieldType()
                );
            }

            if (answer.quality()
                    == HhQuestionnaireAnswerQuality.PROFILE_DERIVED) {
                profileDerivedFilledCount++;
            } else if (answer.quality()
                    == HhQuestionnaireAnswerQuality.SAFE_DEFAULT) {
                safeDefaultFilledCount++;
            } else {
                confirmedFilledCount++;
            }
        }

        return new QuestionnaireFillSummary(
                confirmedFilledCount,
                profileDerivedFilledCount,
                safeDefaultFilledCount,
                0
        );
    }

    private static @NonNull Map<String, GeneratedHhQuestionnaireAnswerDto>
    getStringGeneratedHhQuestionnaireAnswerDtoMap(
            List<HhQuestionnaireQuestionDto> questions,
            List<GeneratedHhQuestionnaireAnswerDto> answers)
    {
        Map<String, GeneratedHhQuestionnaireAnswerDto> answersByField =
                new HashMap<>();

        for (GeneratedHhQuestionnaireAnswerDto answer : answers) {
            GeneratedHhQuestionnaireAnswerDto previous =
                    answersByField.put(
                            answer.fieldName(),
                            answer
                    );

            if (previous != null) {
                throw new HhAutoResponseExecutionException(
                        "LLM returned duplicate questionnaire answer: "
                                + answer.fieldName()
                );
            }
        }

        if (answersByField.size() != questions.size()) {
            throw new HhAutoResponseExecutionException(
                    "LLM did not return answers for every questionnaire field"
            );
        }
        return answersByField;
    }

    private void fillTextQuestionnaireField(
            WebDriver driver,
            HhQuestionnaireQuestionDto question,
            GeneratedHhQuestionnaireAnswerDto answer
    ) {
        if (answer.answer().isBlank()) {
            throw new HhAutoResponseExecutionException(
                    "Questionnaire text answer is blank for field: "
                            + question.fieldName()
            );
        }

        By fieldSelector = By.cssSelector(
                "textarea[name='" + question.fieldName() + "']"
        );

        WebElement textarea = findFirstVisibleWithDeadline(
                driver,
                List.of(fieldSelector),
                properties.waitTimeout()
        ).orElseThrow(() -> new HhAutoResponseExecutionException(
                "HH questionnaire textarea was not found: "
                        + question.fieldName()
        ));

        fillQuestionnaireTextarea(
                driver,
                question.fieldName(),
                answer.answer()
        );

        String actualValue = textarea.getDomProperty("value");

        if (actualValue == null
                || !actualValue.strip().equals(answer.answer().strip())) {
            throw new HhAutoResponseExecutionException(
                    "HH questionnaire answer was not inserted into field: "
                            + question.fieldName()
            );
        }
    }

    private void fillRadioQuestionnaireField(
            WebDriver driver,
            HhQuestionnaireQuestionDto question,
            GeneratedHhQuestionnaireAnswerDto answer
    ) {
        if (!answer.answer().isBlank()) {
            throw new HhAutoResponseExecutionException(
                    "Regular radio answer must not contain text: "
                            + question.fieldName()
            );
        }

        selectRadioQuestionnaireOption(
                driver,
                question,
                answer.selectedOptionValue()
        );
    }

    private void fillRadioWithOtherTextQuestionnaireField(
            WebDriver driver,
            HhQuestionnaireQuestionDto question,
            GeneratedHhQuestionnaireAnswerDto answer
    ) {
        String selectedOptionValue = compact(
                answer.selectedOptionValue()
        );

        selectRadioQuestionnaireOption(
                driver,
                question,
                selectedOptionValue
        );

        boolean otherOptionSelected = selectedOptionValue.equals(
                question.otherOptionValue()
        );

        if (!otherOptionSelected) {
            if (!answer.answer().isBlank()) {
                throw new HhAutoResponseExecutionException(
                        "Text answer is allowed only for other radio option: "
                                + question.fieldName()
                );
            }

            return;
        }

        if (answer.answer().isBlank()) {
            throw new HhAutoResponseExecutionException(
                    "Other radio option requires textarea content: "
                            + question.fieldName()
            );
        }

        fillQuestionnaireTextarea(
                driver,
                question.otherTextFieldName(),
                answer.answer()
        );
    }

    private void selectRadioQuestionnaireOption(
            WebDriver driver,
            HhQuestionnaireQuestionDto question,
            String selectedOptionValue
    ) {
        String normalizedOptionValue = compact(
                selectedOptionValue
        );

        if (normalizedOptionValue.isBlank()) {
            throw new HhAutoResponseExecutionException(
                    "Radio questionnaire answer does not contain option value: "
                            + question.fieldName()
            );
        }

        boolean optionExistsInQuestion = question.options().stream()
                .anyMatch(option ->
                        option.value().equals(normalizedOptionValue)
                );

        if (!optionExistsInQuestion) {
            throw new HhAutoResponseExecutionException(
                    "LLM selected absent questionnaire option: "
                            + normalizedOptionValue
            );
        }

        By radioSelector = By.cssSelector(
                "input[type='radio'][name='"
                        + question.fieldName()
                        + "']"
        );

        WebElement selectedRadio = driver.findElements(radioSelector)
                .stream()
                .filter(radio ->
                        normalizedOptionValue.equals(
                                compact(
                                        radio.getDomAttribute("value")
                                )
                        )
                )
                .findFirst()
                .orElseThrow(() -> new HhAutoResponseExecutionException(
                        "HH questionnaire radio option was not found: "
                                + "field="
                                + question.fieldName()
                                + ", value="
                                + normalizedOptionValue
                ));

        scrollIntoView(driver, selectedRadio);
        clickWithJavaScript(driver, selectedRadio);

        if (!selectedRadio.isSelected()) {
            throw new HhAutoResponseExecutionException(
                    "HH questionnaire radio option was not selected: "
                            + "field="
                            + question.fieldName()
                            + ", value="
                            + normalizedOptionValue
            );
        }
    }

    private void fillQuestionnaireTextarea(
            WebDriver driver,
            String fieldName,
            String value
    ) {
        By fieldSelector = By.cssSelector(
                "textarea[name='" + fieldName + "']"
        );

        WebElement textarea = findFirstVisibleWithDeadline(
                driver,
                List.of(fieldSelector),
                properties.waitTimeout()
        ).orElseThrow(() -> new HhAutoResponseExecutionException(
                "HH questionnaire textarea was not found: " + fieldName
        ));

        fillTextarea(driver, textarea, value);

        String actualValue = textarea.getDomProperty("value");

        if (actualValue == null
                || !actualValue.strip().equals(value.strip())) {
            throw new HhAutoResponseExecutionException(
                    "HH questionnaire answer was not inserted into field: "
                            + fieldName
            );
        }
    }

    private GeneratedHhQuestionnaireAnswersDto
    generateQuestionnaireAnswers(
            HhQuestionnaireGenerationContext questionnaireContext,
            List<HhQuestionnaireQuestionDto> questions
    ) {
        return generateHhQuestionnaireAnswersUseCase.generate(
                questionnaireContext.toCommand(questions)
        ).join();
    }

    private void saveQuestionnaireReviewDiagnostic(
            BrowserRunContext context,
            List<HhQuestionnaireQuestionDto> questions,
            GeneratedHhQuestionnaireAnswersDto generatedAnswers,
            QuestionnaireFillSummary fillSummary,
            boolean submissionSuppressed,
            String stopReason
    ) {
        try {
            Path runDirectory = Path.of(
                    "logs",
                    "hh-browser-debug",
                    context.executionId().toString()
            ).toAbsolutePath().normalize();

            Files.createDirectories(runDirectory);

            List<QuestionnaireReviewDiagnosticItem> items = getQuestionnaireReviewDiagnosticItems(questions, generatedAnswers);

            QuestionnaireReviewDiagnostic diagnostic =
                    new QuestionnaireReviewDiagnostic(
                            Instant.now().toString(),
                            context.executionId().toString(),
                            context.browserSessionId(),
                            generatedAnswers.provider(),
                            generatedAnswers.model(),
                            fillSummary.confirmedFilledCount(),
                            fillSummary.profileDerivedFilledCount(),
                            fillSummary.safeDefaultFilledCount(),
                            fillSummary.reviewSkippedCount(),
                            questionnaireExecutionProperties.executionMode().name(),
                            submissionSuppressed,
                            stopReason == null ? "" : stopReason.strip(),
                            items
                    );

            Path diagnosticPath = runDirectory.resolve(
                    "questionnaire-review.json"
            );

            Files.writeString(
                    diagnosticPath,
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(diagnostic),
                    StandardCharsets.UTF_8
            );

            log.info(
                    "HH questionnaire review diagnostic saved: "
                            + "executionId={}, sessionId={}, path={}",
                    context.executionId(),
                    context.browserSessionId(),
                    diagnosticPath
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save HH questionnaire review diagnostic: "
                            + "executionId={}, sessionId={}",
                    context.executionId(),
                    context.browserSessionId(),
                    exception
            );
        }
    }

    private static @NonNull List<QuestionnaireReviewDiagnosticItem> getQuestionnaireReviewDiagnosticItems(List<HhQuestionnaireQuestionDto> questions, GeneratedHhQuestionnaireAnswersDto generatedAnswers) {
        Map<String, HhQuestionnaireQuestionDto> questionsByField =
                new LinkedHashMap<>();

        for (HhQuestionnaireQuestionDto question : questions) {
            questionsByField.put(
                    question.fieldName(),
                    question
            );
        }

        List<QuestionnaireReviewDiagnosticItem> items =
                new ArrayList<>();

        for (GeneratedHhQuestionnaireAnswerDto answer
                : generatedAnswers.answers()) {
            HhQuestionnaireQuestionDto question =
                    questionsByField.get(answer.fieldName());

            items.add(
                    new QuestionnaireReviewDiagnosticItem(
                            answer.fieldName(),
                            question == null
                                    ? ""
                                    : question.questionText(),
                            question == null
                                    ? ""
                                    : question.fieldType().name(),
                            question == null
                                    ? List.of()
                                    : question.options(),
                            answer.answer(),
                            answer.selectedOptionValue(),
                            answer.quality().name(),
                            answer.reviewReason(),
                            answer.evidence()
                    )
            );
        }
        return items;
    }

    private boolean containsQuestionnaireGenerationUnavailable(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (current
                    instanceof QuestionnaireAnswerGenerationUnavailableException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private boolean hasQuestionForm(WebDriver driver) {
        return !driver.findElements(QUESTIONNAIRE_TASK_BODY_SELECTOR).isEmpty();
    }

    private void waitForResponseSent(WebDriver driver) {
        long deadline = System.nanoTime() + properties.waitTimeout().toNanos();

        while (System.nanoTime() < deadline) {
            if (isResponseAlreadySent(driver)) {
                return;
            }

            sleepSilently(300);
        }

        throw new HhAutoResponseExecutionException(
                "HH submit was clicked, but response success state was not detected. Current URL: "
                        + driver.getCurrentUrl()
        );
    }

    private void ensureLoggedIn(WebDriver driver) {
        Object userType = ((JavascriptExecutor) driver)
                .executeScript("return window.globalVars && window.globalVars.userType;");

        Object login = ((JavascriptExecutor) driver)
                .executeScript("return window.globalVars && window.globalVars.login;");

        if (userType == null
                || String.valueOf(userType).isBlank()
                || "anonymous".equalsIgnoreCase(String.valueOf(userType))) {
            throw new HhAutoResponseExecutionException(
                    "HH.ru browser profile is not authenticated. Log in to HH.ru manually in the Selenium Chrome profile and retry."
            );
        }

        if (login == null || String.valueOf(login).isBlank()) {
            throw new HhAutoResponseExecutionException(
                    "HH.ru browser profile does not contain authenticated login. Log in manually and retry."
            );
        }
    }

    private void waitUntilPageLoaded(WebDriver driver, WebDriverWait wait) {
        wait.until(webDriver -> {
            Object readyState = ((JavascriptExecutor) driver).executeScript("return document.readyState");
            String state = String.valueOf(readyState);
            return "interactive".equals(state) || "complete".equals(state);
        });
    }

    private Optional<WebElement> findFirstVisibleWithDeadline(
            WebDriver driver,
            List<By> selectors,
            Duration timeout
    ) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            Optional<WebElement> element = findFirstVisibleNow(driver, selectors);

            if (element.isPresent()) {
                return element;
            }

            sleepSilently(250);
        }

        return Optional.empty();
    }

    private Optional<WebElement> findFirstVisibleNow(WebDriver driver, List<By> selectors) {
        for (By selector : selectors) {
            List<WebElement> elements = driver.findElements(selector);

            for (WebElement element : elements) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return Optional.of(element);
                    }
                } catch (NoSuchElementException ignored) {
                    // Element disappeared, try next one.
                }
            }
        }

        return Optional.empty();
    }

    private HhBrowserAutoResponseOutcome
    tryAttachPostResponseCoverLetter(
            WebDriver driver,
            String coverLetter,
            BrowserRunContext context
    ) {
        saveDiagnosticsOnce(
                driver,
                context,
                "post-response-letter-attachment-available"
        );

        Optional<WebElement> attachButton =
                findFirstVisibleWithDeadline(
                        driver,
                        POST_RESPONSE_ATTACH_COVER_LETTER_SELECTORS,
                        properties.waitTimeout()
                );

        if (attachButton.isEmpty()) {
            log.warn(
                    "HH response was already sent, but the exact post-response "
                            + "cover letter attachment button was not found: "
                            + "executionId={}, sessionId={}",
                    context.executionId(),
                    context.browserSessionId()
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-attachment-button-not-found"
            );

            return HhBrowserAutoResponseOutcome
                    .RESPONSE_SENT_WITHOUT_COVER_LETTER;
        }

        try {
            WebElement button = attachButton.get();

            scrollIntoView(driver, button);
            clickWithJavaScript(driver, button);

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-attachment-clicked"
            );

            WebElement textarea = findFirstVisibleWithDeadline(
                    driver,
                    POST_RESPONSE_LETTER_TEXTAREA_SELECTORS,
                    properties.waitTimeout()
            ).orElse(null);

            if (textarea == null) {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "post-response-letter-form-not-found"
                );

                return HhBrowserAutoResponseOutcome
                        .RESPONSE_SENT_WITHOUT_COVER_LETTER;
            }

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-form-opened"
            );

            fillCoverLetterTextareaAndVerify(
                    driver,
                    textarea,
                    coverLetter,
                    context
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-dom-verified"
            );

            WebElement submitButton = findFirstVisibleWithDeadline(
                    driver,
                    POST_RESPONSE_LETTER_SUBMIT_SELECTORS,
                    properties.waitTimeout()
            ).orElse(null);

            if (submitButton == null) {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "post-response-letter-submit-not-found"
                );

                return HhBrowserAutoResponseOutcome
                        .RESPONSE_SENT_WITHOUT_COVER_LETTER;
            }

            scrollIntoView(driver, submitButton);

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-submit-ready"
            );

            clickWithJavaScript(driver, submitButton);

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-submit-clicked"
            );

            if (waitForPostResponseLetterConfirmation(driver)) {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "post-response-letter-confirmed"
                );

                log.info(
                        "HH post-response cover letter was confirmed: "
                                + "executionId={}, sessionId={}",
                        context.executionId(),
                        context.browserSessionId()
                );

                return HhBrowserAutoResponseOutcome
                        .RESPONSE_SENT_WITH_COVER_LETTER;
            }

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-confirmation-not-found"
            );

            return HhBrowserAutoResponseOutcome
                    .RESPONSE_SENT_WITHOUT_COVER_LETTER;
        } catch (RuntimeException exception) {
            log.warn(
                    "HH post-response cover letter attachment failed after "
                            + "the base response was already sent: "
                            + "executionId={}, sessionId={}, reason={}",
                    context.executionId(),
                    context.browserSessionId(),
                    rootMessage(exception)
            );

            saveDiagnosticsOnce(
                    driver,
                    context,
                    "post-response-letter-attachment-failed"
            );

            return HhBrowserAutoResponseOutcome
                    .RESPONSE_SENT_WITHOUT_COVER_LETTER;
        }
    }

    private boolean waitForPostResponseLetterConfirmation(
            WebDriver driver
    ) {
        long deadline = System.nanoTime()
                + properties.waitTimeout().toNanos();

        while (System.nanoTime() < deadline) {
            if (findFirstVisibleNow(
                    driver,
                    POST_RESPONSE_LETTER_SUCCESS_SELECTORS
            ).isPresent()) {
                return true;
            }

            sleepSilently(250);
        }

        return false;
    }

    private void fillCoverLetterTextareaAndVerify(
            WebDriver driver,
            WebElement textarea,
            String expectedCoverLetter,
            BrowserRunContext context
    ) {
        try {
            scrollIntoView(driver, textarea);

            textarea.clear();

            String valueAfterClear = readTextareaValue(
                    driver,
                    textarea
            );

            if (!isTextareaValueBlank(valueAfterClear)) {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "cover-letter-clear-not-verified"
                );

                throw new HhAutoResponseExecutionException(
                        "HH cover letter textarea was not cleared before input. "
                                + "Strict policy prohibited response submission."
                );
            }

            textarea.sendKeys(expectedCoverLetter);

            String actualValue = readTextareaValue(
                    driver,
                    textarea
            );

            HhCoverLetterDomValueVerification verification =
                    HhCoverLetterDomValueVerifier.verify(
                            expectedCoverLetter,
                            actualValue
                    );

            if (!verification.matches()) {
                saveDiagnosticsOnce(
                        driver,
                        context,
                        "cover-letter-value-not-verified"
                );

                throw new HhAutoResponseExecutionException(
                        "HH cover letter DOM value was not verified after input. "
                                + "Strict policy prohibited response submission. "
                                + "expectedLength="
                                + verification.expectedLength()
                                + ", actualLength="
                                + verification.actualLength()
                );
            }

            log.info(
                    "HH cover letter DOM value verified: executionId={}, "
                            + "sessionId={}, expectedLength={}, actualLength={}",
                    context.executionId(),
                    context.browserSessionId(),
                    verification.expectedLength(),
                    verification.actualLength()
            );
        } catch (HhAutoResponseExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            saveDiagnosticsOnce(
                    driver,
                    context,
                    "cover-letter-dom-write-failed"
            );

            throw exception;
        }
    }

    private String readTextareaValue(
            WebDriver driver,
            WebElement textarea
    ) {
        Object value = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].value;",
                textarea
        );

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private boolean isTextareaValueBlank(String value) {
        return value == null
                || value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .strip()
                .isBlank();
    }

    private void fillTextarea(WebDriver driver, WebElement textarea, String value) {
        scrollIntoView(driver, textarea);
        textarea.clear();
        textarea.sendKeys(value);
    }

    private void scrollIntoView(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                element
        );
    }

    private void clickWithJavaScript(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private String rootMessage(
            Throwable throwable
    ) {
        if (throwable == null) {
            return "unknown error";
        }

        Throwable current = throwable;

        while (current.getCause() != null
                && current.getCause() != current) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new HhAutoResponseExecutionException("HH browser agent was interrupted", exception);
        }
    }

    private String compact(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ").trim();
    }

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new HhAutoResponseExecutionException(message);
        }

        return value.trim();
    }

    private static final class BrowserRunContext {

        private final UUID executionId;
        private final String externalVacancyId;
        private final String browserSessionId;
        private final String diagnosticsRelativeDirectory;
        private String candidateApprovalReason;
        private final Set<String> savedDiagnosticReasons =
                new LinkedHashSet<>();

        private BrowserRunContext(
                UUID executionId,
                String externalVacancyId,
                String browserSessionId
        ) {
            this.executionId = executionId;
            this.externalVacancyId = externalVacancyId;
            this.browserSessionId = browserSessionId;
            this.diagnosticsRelativeDirectory = Path.of(
                    "logs",
                    "hh-browser-debug",
                    HhBrowserAutoResponseAgent.safePathSegment(
                            externalVacancyId
                    ),
                    executionId.toString()
            ).toString();
        }

        private UUID executionId() {
            return executionId;
        }

        private String externalVacancyId() {
            return externalVacancyId;
        }

        private boolean markDiagnosticsSaved(String reason) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "HH browser diagnostic reason must not be blank"
                );
            }

            return savedDiagnosticReasons.add(reason.trim());
        }

        private String browserSessionId() {
            return browserSessionId;
        }

        private String diagnosticsRelativeDirectory() {
            return diagnosticsRelativeDirectory;
        }

        private String candidateApprovalReason() {
            return candidateApprovalReason;
        }

        private void markCandidateApprovalRequired(
                String reason
        ) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Candidate approval reason must not be blank"
                );
            }

            candidateApprovalReason = reason.trim();
        }
    }

    private record OtherTextBinding(
            String otherOptionValue,
            String otherTextFieldName
    ) {
    }

    private record QuestionnaireFillSummary(
            int confirmedFilledCount,
            int profileDerivedFilledCount,
            int safeDefaultFilledCount,
            int reviewSkippedCount
    ) {
    }

    private record QuestionnaireReviewDiagnostic(
            String generatedAt,
            String executionId,
            String browserSessionId,
            String provider,
            String model,
            int confirmedFilledCount,
            int profileDerivedFilledCount,
            int safeDefaultFilledCount,
            int reviewSkippedCount,
            String executionMode,
            boolean submissionSuppressed,
            String stopReason,
            List<QuestionnaireReviewDiagnosticItem> answers
    ) {
    }

    private record QuestionnaireReviewDiagnosticItem(
            String fieldName,
            String questionText,
            String fieldType,
            List<HhQuestionnaireOptionDto> options,
            String answer,
            String selectedOptionValue,
            String quality,
            String reviewReason,
            List<String> evidence
    ) {
    }
}