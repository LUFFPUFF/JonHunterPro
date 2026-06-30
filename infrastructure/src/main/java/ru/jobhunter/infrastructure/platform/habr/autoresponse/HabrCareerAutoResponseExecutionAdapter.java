package ru.jobhunter.infrastructure.platform.habr.autoresponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AutoResponseExecutionRequest;
import ru.jobhunter.core.application.dto.AutoResponseExecutionResultDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsDto;
import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsProbeResultDto;
import ru.jobhunter.core.application.dto.PrimaryResumeContentDto;
import ru.jobhunter.core.application.port.out.AutoResponseExecutionPort;
import ru.jobhunter.core.application.usecase.coverletter.CoverLetterQualityValidator;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterCommand;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterUseCase;
import ru.jobhunter.core.application.usecase.coverletter.GeneratedCoverLetterQualityException;
import ru.jobhunter.core.application.usecase.integration.ProbeHabrCareerVacancyDetailsUseCase;
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
public final class HabrCareerAutoResponseExecutionAdapter
        implements AutoResponseExecutionPort {

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerAutoResponseExecutionAdapter.class
    );

    private static final String TEMPORARY_LLM_UNAVAILABLE_MESSAGE =
            "LLM временно недоступна. Вакансия оставлена "
                    + "в статусе READY, повторите позже.";

    private final HabrCareerBrowserAutoResponseAgent browserAgent;
    private final HabrCareerBrowserAutoResponseProperties properties;
    private final ProbeHabrCareerVacancyDetailsUseCase vacancyDetailsUseCase;
    private final GetPrimaryResumeContentUseCase getPrimaryResumeContentUseCase;
    private final GenerateCoverLetterUseCase generateCoverLetterUseCase;
    private final GeneralCoverLetterFallbackResolver generalCoverLetterFallbackResolver;
    private final ExecutorService executorService;

    public HabrCareerAutoResponseExecutionAdapter(
            HabrCareerBrowserAutoResponseAgent browserAgent,
            HabrCareerBrowserAutoResponseProperties properties,
            ProbeHabrCareerVacancyDetailsUseCase vacancyDetailsUseCase,
            GetPrimaryResumeContentUseCase getPrimaryResumeContentUseCase,
            GenerateCoverLetterUseCase generateCoverLetterUseCase,
            GeneralCoverLetterFallbackResolver generalCoverLetterFallbackResolver,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.browserAgent = Objects.requireNonNull(
                browserAgent,
                "Habr Career browser auto response agent must not be null"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career browser auto response properties must not be null"
        );
        this.vacancyDetailsUseCase = Objects.requireNonNull(
                vacancyDetailsUseCase,
                "Habr Career vacancy details use case must not be null"
        );
        this.getPrimaryResumeContentUseCase = Objects.requireNonNull(
                getPrimaryResumeContentUseCase,
                "Get primary resume content use case must not be null"
        );
        this.generateCoverLetterUseCase = Objects.requireNonNull(
                generateCoverLetterUseCase,
                "Generate cover letter use case must not be null"
        );
        this.generalCoverLetterFallbackResolver = Objects.requireNonNull(
                generalCoverLetterFallbackResolver,
                "General cover letter fallback resolver must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Application task executor must not be null"
        );
    }

    @Override
    public boolean supports(VacancySource source) {
        return VacancySource.HABR_CAREER == source;
    }

    @Override
    public CompletableFuture<AutoResponseExecutionResultDto> execute(
            AutoResponseExecutionRequest request
    ) {
        Objects.requireNonNull(request, "Auto response request must not be null");

        if (!supports(request.source())) {
            return CompletableFuture.completedFuture(
                    AutoResponseExecutionResultDto.notAvailable(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            "Habr Career auto-response adapter does not support source: "
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
                            "Автоотклики Habr Career отключены. Установите "
                                    + "HABR_CAREER_BROWSER_AUTO_RESPONSE_ENABLED=true "
                                    + "для запуска."
                    )
            );
        }

        UUID executionId = UUID.randomUUID();

        log.info(
                "Preparing Habr Career auto response: executionId={}, userId={}, "
                        + "queueItemId={}, externalVacancyId={}, mode={}",
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
                .exceptionally(throwable -> toPreparationFailureResult(
                        request,
                        executionId,
                        throwable
                ));
    }

    private CompletableFuture<PreparedHabrAutoResponse> prepareAutoResponse(
            AutoResponseExecutionRequest request
    ) {
        CompletableFuture<HabrCareerVacancyDetailsProbeResultDto> detailsFuture =
                vacancyDetailsUseCase.probe(
                        request.userId(),
                        request.externalVacancyId()
                );

        CompletableFuture<Optional<PrimaryResumeContentDto>> resumeFuture =
                getPrimaryResumeContentUseCase.getPrimaryResumeContent(
                        request.userId()
                );

        return detailsFuture.thenCombine(
                        resumeFuture,
                        (detailsResult, optionalResume) -> new CoverLetterContext(
                                requireVacancyDetails(detailsResult),
                                requirePrimaryResume(optionalResume)
                        )
                )
                .thenCompose(context -> prepareCoverLetter(request, context));
    }

    private CompletableFuture<PreparedHabrAutoResponse> prepareCoverLetter(
            AutoResponseExecutionRequest request,
            CoverLetterContext context
    ) {
        HabrCareerVacancyDetailsDto vacancy = context.vacancy();

        GenerateCoverLetterCommand command = new GenerateCoverLetterCommand(
                request.userId(),
                request.source(),
                vacancy.externalVacancyId(),
                vacancy.title(),
                valueOrUnknown(vacancy.companyName()),
                vacancy.vacancyUrl(),
                buildVacancyContext(vacancy),
                context.resume().content()
        );

        return generateCoverLetterUseCase.generate(command)
                .thenApply(generated -> new PreparedHabrAutoResponse(
                        vacancy,
                        requireGeneratedCoverLetter(generated.content()),
                        CoverLetterSource.LLM_GENERATED
                ))
                .exceptionallyCompose(throwable ->
                        resolveGeneralFallbackAfterLlmFailure(
                                request,
                                context,
                                throwable
                        ));
    }

    private CompletableFuture<PreparedHabrAutoResponse>
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

        return generalCoverLetterFallbackResolver.resolve(request.userId())
                .handle((optionalFallback, fallbackThrowable) ->
                        fallbackThrowable == null
                                ? optionalFallback
                                : Optional.<String>empty())
                .thenCompose(optionalFallback -> {
                    if (optionalFallback.isEmpty()) {
                        return CompletableFuture.failedFuture(providerFailure);
                    }

                    return CompletableFuture.completedFuture(
                            new PreparedHabrAutoResponse(
                                    context.vacancy(),
                                    optionalFallback.get(),
                                    CoverLetterSource.USER_GENERAL_FALLBACK
                            )
                    );
                });
    }

    private AutoResponseExecutionResultDto executeBrowserAutoResponse(
            AutoResponseExecutionRequest request,
            PreparedHabrAutoResponse prepared,
            UUID executionId
    ) {
        try {
            HabrCareerBrowserAutoResponseResult browserResult =
                    browserAgent.apply(
                            request.userId(),
                            request.externalVacancyId(),
                            firstNonBlank(
                                    prepared.vacancy().vacancyUrl(),
                                    request.vacancyUrl()
                            ),
                            prepared.coverLetter(),
                            executionId
                    );

            return toExecutionResult(
                    request,
                    browserResult,
                    prepared.coverLetterSource()
            );
        } catch (RuntimeException exception) {
            if (containsLlmProviderUnavailable(exception)) {
                return temporarilyUnavailableResult(
                        request,
                        TEMPORARY_LLM_UNAVAILABLE_MESSAGE
                );
            }

            return AutoResponseExecutionResultDto.failed(
                    request.queueItemId(),
                    request.source(),
                    request.externalVacancyId(),
                    rootMessage(exception)
            );
        }
    }

    private AutoResponseExecutionResultDto toPreparationFailureResult(
            AutoResponseExecutionRequest request,
            UUID executionId,
            Throwable throwable
    ) {
        if (containsLlmProviderUnavailable(throwable)) {
            log.warn(
                    "Habr Career cover letter is temporarily unavailable: "
                            + "executionId={}, queueItemId={}",
                    executionId,
                    request.queueItemId()
            );
            return temporarilyUnavailableResult(
                    request,
                    TEMPORARY_LLM_UNAVAILABLE_MESSAGE
            );
        }

        log.warn(
                "Habr Career auto response preparation failed: executionId={}, "
                        + "queueItemId={}",
                executionId,
                request.queueItemId(),
                throwable
        );
        return AutoResponseExecutionResultDto.failed(
                request.queueItemId(),
                request.source(),
                request.externalVacancyId(),
                rootMessage(throwable)
        );
    }

    private AutoResponseExecutionResultDto toExecutionResult(
            AutoResponseExecutionRequest request,
            HabrCareerBrowserAutoResponseResult browserResult,
            CoverLetterSource coverLetterSource
    ) {
        if (browserResult.requiresCandidateApproval()) {
            return AutoResponseExecutionResultDto.candidateApprovalRequired(
                    request.queueItemId(),
                    request.source(),
                    request.externalVacancyId(),
                    browserResult.candidateApprovalReason(),
                    browserResult.diagnosticDirectory()
            );
        }

        return switch (browserResult.outcome()) {
            case RESPONSE_SENT_WITH_COVER_LETTER ->
                    AutoResponseExecutionResultDto.success(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            browserResult.message()
                                    + " Источник письма: "
                                    + coverLetterSource.userVisibleName()
                                    + "."
                    );
            case RESPONSE_SENT_WITHOUT_COVER_LETTER ->
                    AutoResponseExecutionResultDto.partialSuccess(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            browserResult.message()
                    );
            case ALREADY_RESPONDED ->
                    AutoResponseExecutionResultDto.alreadyResponded(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            browserResult.message()
                    );
            case PREFLIGHT_VERIFIED ->
                    AutoResponseExecutionResultDto.preflightCompleted(
                            request.queueItemId(),
                            request.source(),
                            request.externalVacancyId(),
                            browserResult.message()
                    );
            case CANDIDATE_APPROVAL_REQUIRED -> throw new IllegalStateException(
                    "Candidate approval result must be handled before mapping"
            );
        };
    }

    private HabrCareerVacancyDetailsDto requireVacancyDetails(
            HabrCareerVacancyDetailsProbeResultDto result
    ) {
        if (result.status()
                != HabrCareerVacancyDetailsProbeResultDto.Status
                .VACANCY_DETAILS_READY
                || result.vacancy() == null) {
            throw new HabrCareerAutoResponseExecutionException(
                    "Не удалось получить детали вакансии Habr Career: "
                            + result.status()
            );
        }

        return result.vacancy();
    }

    private PrimaryResumeContentDto requirePrimaryResume(
            Optional<PrimaryResumeContentDto> optionalResume
    ) {
        return optionalResume.orElseThrow(() -> new IllegalStateException(
                "Основное резюме не найдено. Загрузите PDF перед автооткликом."
        ));
    }

    private String requireGeneratedCoverLetter(String coverLetter) {
        try {
            return CoverLetterQualityValidator.validateAndNormalize(coverLetter);
        } catch (GeneratedCoverLetterQualityException exception) {
            throw new LlmProviderUnavailableException(
                    "cover-letter-quality",
                    LlmFailureCategory.INVALID_MODEL_OUTPUT,
                    "Generated cover letter was rejected by quality gate: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String buildVacancyContext(HabrCareerVacancyDetailsDto vacancy) {
        StringBuilder builder = new StringBuilder(vacancy.description());
        appendContextLine(builder, "Ключевые навыки", formatSkills(vacancy.skills()));
        appendContextLine(builder, "Город", vacancy.city());
        appendContextLine(builder, "Тип занятости", vacancy.employmentType());
        appendContextLine(builder, "Зарплата", vacancy.salary());
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
        return skills == null || skills.isEmpty()
                ? null
                : String.join(", ", skills);
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

    private boolean containsLlmProviderUnavailable(Throwable throwable) {
        return findLlmProviderUnavailable(throwable) != null;
    }

    private LlmProviderUnavailableException findLlmProviderUnavailable(
            Throwable throwable
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof LlmProviderUnavailableException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        throw new HabrCareerAutoResponseExecutionException(
                "Habr Career vacancy URL must not be blank"
        );
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "не указана" : value.trim();
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
            HabrCareerVacancyDetailsDto vacancy,
            PrimaryResumeContentDto resume
    ) {
    }

    private record PreparedHabrAutoResponse(
            HabrCareerVacancyDetailsDto vacancy,
            String coverLetter,
            CoverLetterSource coverLetterSource
    ) {
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
}
