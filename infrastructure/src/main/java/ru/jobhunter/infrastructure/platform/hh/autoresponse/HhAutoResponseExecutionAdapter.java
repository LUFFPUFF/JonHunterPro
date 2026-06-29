package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AutoResponseExecutionRequest;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.HhVacancyDetailsDto;
import ru.jobhunter.core.application.dto.PrimaryResumeContentDto;
import ru.jobhunter.core.application.port.out.AutoResponseExecutionPort;
import ru.jobhunter.core.application.usecase.coverletter.CoverLetterQualityValidator;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterCommand;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterUseCase;
import ru.jobhunter.core.application.usecase.coverletter.GeneratedCoverLetterQualityException;
import ru.jobhunter.core.application.usecase.integration.GetHhVacancyDetailsUseCase;
import ru.jobhunter.core.application.usecase.resume.GetPrimaryResumeContentUseCase;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.infrastructure.llm.routing.LlmFailureCategory;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;
import ru.jobhunter.infrastructure.service.GeneralCoverLetterFallbackResolver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public final class HhAutoResponseExecutionAdapter implements AutoResponseExecutionPort {

    private static final Logger log =
            LoggerFactory.getLogger(HhAutoResponseExecutionAdapter.class);

    private static final String TEMPORARY_LLM_UNAVAILABLE_MESSAGE =
            "LLM временно недоступна. Вакансия оставлена "
                    + "в статусе READY, повторите позже.";

    private final HhBrowserAutoResponseAgent browserAgent;
    private final HhBrowserAutoResponseProperties properties;
    private final GetHhVacancyDetailsUseCase getHhVacancyDetailsUseCase;
    private final GetPrimaryResumeContentUseCase getPrimaryResumeContentUseCase;
    private final GenerateCoverLetterUseCase generateCoverLetterUseCase;
    private final GeneralCoverLetterFallbackResolver generalCoverLetterFallbackResolver;
    private final ExecutorService executorService;

    public HhAutoResponseExecutionAdapter(
            HhBrowserAutoResponseAgent browserAgent,
            HhBrowserAutoResponseProperties properties,
            GetHhVacancyDetailsUseCase getHhVacancyDetailsUseCase,
            GetPrimaryResumeContentUseCase getPrimaryResumeContentUseCase,
            GenerateCoverLetterUseCase generateCoverLetterUseCase,
            GeneralCoverLetterFallbackResolver generalCoverLetterFallbackResolver,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.browserAgent = Objects.requireNonNull(
                browserAgent,
                "HH browser auto response agent must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "HH browser auto response properties must not be null"
        );
        this.getHhVacancyDetailsUseCase = Objects.requireNonNull(
                getHhVacancyDetailsUseCase,
                "Get HH vacancy details use case must not be null"
        );
        this.getPrimaryResumeContentUseCase = Objects.requireNonNull(
                getPrimaryResumeContentUseCase,
                "Get primary resume content use case must not be null"
        );
        this.generateCoverLetterUseCase = Objects.requireNonNull(
                generateCoverLetterUseCase,
                "Generate cover letter use case must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Executor service must not be null"
        );
        this.generalCoverLetterFallbackResolver =
                Objects.requireNonNull(
                        generalCoverLetterFallbackResolver,
                        "General cover letter fallback resolver must not be null"
                );
    }

    @Override
    public boolean supports(VacancySource source) {
        return VacancySource.HH_RU == source;
    }

    @Override
    public CompletableFuture<AutoResponseExecutionResultDto> execute(
            AutoResponseExecutionRequest request
    ) {
        Objects.requireNonNull(
                request,
                "Auto response execution request must not be null"
        );

        UUID executionId = UUID.randomUUID();

        if (!supports(request.source())) {
            return CompletableFuture.completedFuture(
                    AutoResponseExecutionResultDto.notAvailable(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            "HH.ru browser auto response adapter does not "
                                    + "support source: "
                                    + request.source()
                    )
            );
        }

        if (!properties.enabled()) {
            return CompletableFuture.completedFuture(
                    AutoResponseExecutionResultDto.notAvailable(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            "HH.ru browser auto response agent is disabled."
                    )
            );
        }

        log.info(
                "Preparing HH.ru auto response with LLM cover letter: "
                        + "executionId={}, userId={}, queueItemId={}, "
                        + "externalVacancyId={}, dryRun={}",
                executionId,
                request.userId(),
                request.queueItemId(),
                request.externalVacancyId(),
                properties.mode()
        );

        return prepareAutoResponse(request)
                .thenApplyAsync(
                        prepared -> executeBrowserAutoResponse(
                                request,
                                prepared,
                                executionId
                        ),
                        executorService
                )
                .exceptionally(throwable -> {
                    if (containsLlmProviderUnavailable(throwable)) {
                        log.warn(
                                "LLM generation is temporarily unavailable or "
                                        + "did not produce a safe cover letter during "
                                        + "HH.ru auto response preparation: "
                                        + "executionId={}, userId={}, queueItemId={}",
                                executionId,
                                request.userId(),
                                request.queueItemId()
                        );

                        return temporarilyUnavailableResult(
                                request,
                                TEMPORARY_LLM_UNAVAILABLE_MESSAGE
                        );
                    }

                    String errorMessage = rootMessage(throwable);

                    log.warn(
                            "HH.ru auto response preparation failed: "
                                    + "executionId={}, userId={}, queueItemId={}, "
                                    + "externalVacancyId={}, reason={}",
                            executionId,
                            request.userId(),
                            request.queueItemId(),
                            request.externalVacancyId(),
                            errorMessage
                    );

                    return AutoResponseExecutionResultDto.failed(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            errorMessage
                    );
                });
    }

    private CompletableFuture<PreparedHhAutoResponse> prepareAutoResponse(
            AutoResponseExecutionRequest request
    ) {
        CompletableFuture<HhVacancyDetailsDto> vacancyFuture =
                getHhVacancyDetailsUseCase.getDetails(
                        request.externalVacancyId()
                );

        CompletableFuture<Optional<PrimaryResumeContentDto>> resumeFuture =
                getPrimaryResumeContentUseCase.getPrimaryResumeContent(
                        request.userId()
                );

        return vacancyFuture
                .thenCombine(
                        resumeFuture,
                        (vacancy, optionalResume) -> new CoverLetterContext(
                                vacancy,
                                requirePrimaryResume(optionalResume)
                        )
                )
                .thenCompose(context ->
                        prepareCoverLetter(request, context)
                );
    }

    private CompletableFuture<PreparedHhAutoResponse> prepareCoverLetter(
            AutoResponseExecutionRequest request,
            CoverLetterContext context
    ) {
        GenerateCoverLetterCommand command =
                new GenerateCoverLetterCommand(
                        request.userId(),
                        request.source(),
                        context.vacancy().externalId(),
                        context.vacancy().name(),
                        valueOrUnknown(context.vacancy().employerName()),
                        context.vacancy().vacancyUrl(),
                        buildVacancyContext(context.vacancy()),
                        context.resume().content()
                );

        return generateCoverLetterUseCase.generate(command)
                .thenApply(generatedLetter ->
                        new PreparedHhAutoResponse(
                                context.vacancy(),
                                context.resume().content(),
                                requireGeneratedCoverLetter(
                                        generatedLetter.content()
                                ),
                                CoverLetterSource.LLM_GENERATED
                        )
                )
                .exceptionallyCompose(throwable ->
                        resolveGeneralFallbackAfterLlmFailure(
                                request,
                                context,
                                throwable
                        )
                );
    }

    private CompletableFuture<PreparedHhAutoResponse>
    resolveGeneralFallbackAfterLlmFailure(
            AutoResponseExecutionRequest request,
            CoverLetterContext context,
            Throwable throwable
    ) {
        LlmProviderUnavailableException providerFailure =
                findLlmProviderUnavailable(throwable);

        if (providerFailure == null) {
            return CompletableFuture.failedFuture(throwable);
        }

        log.warn(
                "LLM cover letter generation failed. Checking user general "
                        + "fallback: userId={}, queueItemId={}, provider={}, "
                        + "failureCategory={}",
                request.userId(),
                request.queueItemId(),
                providerFailure.providerId(),
                providerFailure.failureCategory()
        );

        return generalCoverLetterFallbackResolver.resolve(request.userId())
                .handle((optionalFallback, fallbackThrowable) -> {
                    if (fallbackThrowable == null) {
                        return optionalFallback;
                    }

                    log.warn(
                            "Unable to load user general cover letter fallback: "
                                    + "userId={}, queueItemId={}, reason={}",
                            request.userId(),
                            request.queueItemId(),
                            rootMessage(fallbackThrowable)
                    );

                    return Optional.<String>empty();
                })
                .thenCompose(optionalFallback -> {
                    if (optionalFallback.isEmpty()) {
                        log.info(
                                "User general cover letter fallback is unavailable: "
                                        + "userId={}, queueItemId={}",
                                request.userId(),
                                request.queueItemId()
                        );

                        return CompletableFuture.failedFuture(
                                providerFailure
                        );
                    }

                    String fallbackCoverLetter = optionalFallback.get();

                    log.info(
                            "Using user general cover letter fallback: "
                                    + "userId={}, queueItemId={}, "
                                    + "coverLetterSource={}, coverLetterLength={}",
                            request.userId(),
                            request.queueItemId(),
                            CoverLetterSource.USER_GENERAL_FALLBACK,
                            fallbackCoverLetter.length()
                    );

                    return CompletableFuture.completedFuture(
                            new PreparedHhAutoResponse(
                                    context.vacancy(),
                                    context.resume().content(),
                                    fallbackCoverLetter,
                                    CoverLetterSource.USER_GENERAL_FALLBACK
                            )
                    );
                });
    }

    private AutoResponseExecutionResultDto executeBrowserAutoResponse(AutoResponseExecutionRequest request, PreparedHhAutoResponse prepared, UUID executionId) {
        String vacancyUrl = firstNonBlank(prepared.vacancy().vacancyUrl(), resolveVacancyUrl(request));
        try {
            log.info(
                    "HH.ru browser auto response started after cover letter preparation: "
                            + "executionId={}, userId={}, queueItemId={}, "
                            + "externalVacancyId={}, coverLetterSource={}, "
                            + "coverLetterLength={}",
                    executionId,
                    request.userId(),
                    request.queueItemId(),
                    request.externalVacancyId(),
                    prepared.coverLetterSource(),
                    prepared.coverLetter().length()
            );

            HhQuestionnaireGenerationContext questionnaireContext = new HhQuestionnaireGenerationContext(request.userId(), request.source(), prepared.vacancy(), prepared.resumeText());
            HhBrowserAutoResponseResult browserResult =
                    browserAgent.applyDetailed(
                            request.externalVacancyId(),
                            vacancyUrl,
                            prepared.coverLetter(),
                            questionnaireContext,
                            executionId
                    );

            return toExecutionResult(
                    request,
                    browserResult,
                    prepared.coverLetterSource()
            );
        } catch (HhAutoResponseExecutionException exception) {
            if (containsLlmProviderUnavailable(exception)) {
                return temporarilyUnavailableResult(
                        request,
                        TEMPORARY_LLM_UNAVAILABLE_MESSAGE
                );
            }

            log.warn(
                    "HH.ru browser auto response failed: "
                            + "executionId={}, userId={}, queueItemId={}, "
                            + "externalVacancyId={}",
                    executionId,
                    request.userId(),
                    request.queueItemId(),
                    request.externalVacancyId(),
                    exception
            );

            return AutoResponseExecutionResultDto.failed(
                    request.queueItemId(),
                    request.source(),
                    request.externalVacancyId(),
                    exception.getMessage()
            );
        } catch (RuntimeException exception) {
            if (containsLlmProviderUnavailable(exception)) {
                return temporarilyUnavailableResult(
                        request,
                        TEMPORARY_LLM_UNAVAILABLE_MESSAGE
                );
            }

            log.warn(
                    "HH.ru browser auto response failed unexpectedly: "
                            + "executionId={}, userId={}, queueItemId={}, "
                            + "externalVacancyId={}",
                    executionId,
                    request.userId(),
                    request.queueItemId(),
                    request.externalVacancyId(),
                    exception
            );

            return AutoResponseExecutionResultDto.failed(
                    request.queueItemId(),
                    request.source(),
                    request.externalVacancyId(),
                    rootMessage(exception)
            );
        }
    }

    private PrimaryResumeContentDto requirePrimaryResume(
            Optional<PrimaryResumeContentDto> optionalResume
    ) {
        return optionalResume.orElseThrow(
                () -> new IllegalStateException(
                        "Primary resume was not found. "
                                + "Upload a PDF resume before starting an auto response."
                )
        );
    }

    private String requireGeneratedCoverLetter(
            String coverLetter
    ) {
        try {
            return CoverLetterQualityValidator.validateAndNormalize(
                    coverLetter
            );
        } catch (GeneratedCoverLetterQualityException exception) {
            int generatedLength = coverLetter == null
                    ? 0
                    : coverLetter.strip().length();

            log.warn(
                    "Generated cover letter was rejected by quality gate: "
                            + "coverLetterSource={}, length={}, reason={}",
                    CoverLetterSource.LLM_GENERATED,
                    generatedLength,
                    exception.getMessage()
            );

            throw new LlmProviderUnavailableException(
                    "cover-letter-quality",
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Generated cover letter was rejected by quality gate: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String buildVacancyContext(HhVacancyDetailsDto vacancy) {
        StringBuilder builder = new StringBuilder(
                vacancy.description()
        );

        appendContextLine(
                builder,
                "Ключевые навыки",
                formatSkills(vacancy.keySkills())
        );
        appendContextLine(
                builder,
                "Требуемый опыт",
                vacancy.experienceName()
        );
        appendContextLine(
                builder,
                "Тип занятости",
                vacancy.employmentName()
        );
        appendContextLine(
                builder,
                "График работы",
                vacancy.scheduleName()
        );

        return builder.toString().trim();
    }

    private void appendContextLine(
            StringBuilder builder,
            String label,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        builder.append("\n\n")
                .append(label)
                .append(": ")
                .append(value.trim());
    }

    private String formatSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }

        return String.join(", ", skills);
    }

    private String resolveVacancyUrl(
            AutoResponseExecutionRequest request
    ) {
        if (request.vacancyUrl() != null
                && !request.vacancyUrl().isBlank()) {
            return request.vacancyUrl().trim();
        }

        return "https://hh.ru/vacancy/"
                + request.externalVacancyId();
    }

    private AutoResponseExecutionResultDto toExecutionResult(
            AutoResponseExecutionRequest request,
            HhBrowserAutoResponseResult browserResult,
            CoverLetterSource coverLetterSource
    ) {

        if (browserResult.requiresCandidateApproval()) {
            return AutoResponseExecutionResultDto
                    .candidateApprovalRequired(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            browserResult.candidateApprovalReason(),
                            browserResult.diagnosticDirectory()
                    );
        }

        HhBrowserAutoResponseOutcome outcome =
                browserResult.outcome();

        return switch (outcome) {
            case RESPONSE_SENT_WITH_COVER_LETTER ->
                    AutoResponseExecutionResultDto.success(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            "Отклик и сопроводительное письмо отправлены. "
                                    + "Источник письма: "
                                    + coverLetterSource.userVisibleName()
                                    + "."
                    );

            case RESPONSE_SENT_WITHOUT_COVER_LETTER ->
                    AutoResponseExecutionResultDto.partialSuccess(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            "HH.ru принял отклик и доставил резюме, "
                                    + "но сопроводительное письмо не подтверждено. "
                                    + "Подготовленный источник письма: "
                                    + coverLetterSource.userVisibleName()
                                    + "."
                    );

            case ALREADY_RESPONDED -> AutoResponseExecutionResultDto.alreadyResponded(
                    request.queueItemId(),
                    request.source(),
                    request.externalVacancyId(),
                    "По этой вакансии уже есть отклик на HH.ru."
            );

            case PREFLIGHT_VERIFIED ->
                    AutoResponseExecutionResultDto.preflightCompleted(request.queueItemId(), request.source(), request.externalVacancyId(), "Pre-flight завершён: вакансия открыта, авторизация HH.ru, " + "LLM-письмо и кнопка «Откликнуться» проверены. " + "Никаких действий на HH.ru не выполнено.");

            case QUESTIONNAIRE_REQUIRED -> AutoResponseExecutionResultDto.questionnaireRequired(
                    request.queueItemId(),
                    request.source(),
                    request.externalVacancyId(),
                    "Вакансия содержит вопросы работодателя. "
                            + "Автоотклик не отправлен."
            );

            case QUESTIONNAIRE_FILLED_REVIEW_REQUIRED ->
                    AutoResponseExecutionResultDto.questionnaireFilledReviewRequired(request.queueItemId(), request.source(), request.externalVacancyId(), "LLM заполнила текстовые ответы на вопросы " + "работодателя. Финальная отправка " + "анкеты не выполнена: проверь ответы " + "в сохранённой диагностике.");
        };
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }

        if (second != null && !second.isBlank()) {
            return second.trim();
        }

        throw new IllegalStateException(
                "HH vacancy URL must not be blank"
        );
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank()
                ? "не указана"
                : value.trim();
    }

    private AutoResponseExecutionResultDto temporarilyUnavailableResult(
            AutoResponseExecutionRequest request,
            String message
    ) {
        return AutoResponseExecutionResultDto.notAvailable(
                request.queueItemId(),
                request.source(),
                request.externalVacancyId(),
                message
        );
    }

    private boolean containsLlmProviderUnavailable(
            Throwable throwable
    ) {
        return findLlmProviderUnavailable(throwable) != null;
    }

    private LlmProviderUnavailableException findLlmProviderUnavailable(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof LlmProviderUnavailableException failure) {
                return failure;
            }

            current = current.getCause();
        }

        return null;
    }

    private enum CoverLetterSource {

        LLM_GENERATED("LLM"),
        USER_GENERAL_FALLBACK("общее письмо пользователя");

        private final String userVisibleName;

        CoverLetterSource(String userVisibleName) {
            this.userVisibleName = userVisibleName;
        }

        private String userVisibleName() {
            return userVisibleName;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private record CoverLetterContext(
            HhVacancyDetailsDto vacancy,
            PrimaryResumeContentDto resume
    ) {
    }

    private record PreparedHhAutoResponse(
            HhVacancyDetailsDto vacancy,
            String resumeText,
            String coverLetter,
            CoverLetterSource coverLetterSource
    ) {
    }
}