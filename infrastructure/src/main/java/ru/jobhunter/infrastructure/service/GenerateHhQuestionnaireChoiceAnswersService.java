package ru.jobhunter.infrastructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.CandidateQuestionnaireProfileDto;
import ru.jobhunter.core.application.dto.GenerateHhQuestionnaireAnswersCommand;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswerDto;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswersDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireAnswerQuality;
import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;
import ru.jobhunter.core.application.exception.QuestionnaireAnswerGenerationUnavailableException;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;
import ru.jobhunter.core.application.port.out.llm.LlmPort;
import ru.jobhunter.core.application.usecase.profile.GetCandidateQuestionnaireProfileUseCase;
import ru.jobhunter.core.application.usecase.questionnaire.GenerateHhQuestionnaireChoiceAnswersUseCase;
import ru.jobhunter.infrastructure.prompt.HhChoiceQuestionPromptContext;
import ru.jobhunter.infrastructure.prompt.PromptTemplate;
import ru.jobhunter.infrastructure.prompt.PromptTemplateModel;
import ru.jobhunter.infrastructure.prompt.PromptTemplateRenderer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
public final class GenerateHhQuestionnaireChoiceAnswersService
        implements GenerateHhQuestionnaireChoiceAnswersUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            GenerateHhQuestionnaireChoiceAnswersService.class
    );

    private static final String USE_CASE =
            "answer-hh-questionnaire-choice";

    private static final int MAX_ATTEMPTS = 2;
    private static final int MAX_RESUME_CHARS = 6_000;
    private static final int MAX_REASON_CHARS = 240;

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;
    private final GetCandidateQuestionnaireProfileUseCase
            getCandidateQuestionnaireProfileUseCase;
    private final PromptTemplateRenderer promptTemplateRenderer;

    public GenerateHhQuestionnaireChoiceAnswersService(
            LlmPort llmPort,
            ObjectMapper objectMapper,
            GetCandidateQuestionnaireProfileUseCase
                    getCandidateQuestionnaireProfileUseCase,
            PromptTemplateRenderer promptTemplateRenderer
    ) {
        this.llmPort = Objects.requireNonNull(
                llmPort,
                "LLM port must not be null"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
        this.getCandidateQuestionnaireProfileUseCase =
                Objects.requireNonNull(
                        getCandidateQuestionnaireProfileUseCase,
                        "Candidate profile use case must not be null"
                );
        this.promptTemplateRenderer = Objects.requireNonNull(
                promptTemplateRenderer,
                "Prompt template renderer must not be null"
        );
    }

    @Override
    public CompletableFuture<GeneratedHhQuestionnaireAnswersDto> generate(
            GenerateHhQuestionnaireAnswersCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Questionnaire command must not be null"
        );

        for (HhQuestionnaireQuestionDto question : command.questions()) {
            if (!question.isRadio()) {
                throw new IllegalArgumentException(
                        "Choice answer service supports only RADIO fields: "
                                + question.fieldName()
                );
            }
        }

        return getCandidateQuestionnaireProfileUseCase
                .findByUserId(command.userId())
                .thenCompose(profile ->
                        generateSequentially(command, profile)
                )
                .thenApply(batch ->
                        new GeneratedHhQuestionnaireAnswersDto(
                                batch.answers(),
                                batch.resolvedProvider(),
                                batch.resolvedModel()
                        )
                );
    }

    private CompletableFuture<ChoiceBatch> generateSequentially(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> profile
    ) {
        CompletableFuture<ChoiceBatch> chain =
                CompletableFuture.completedFuture(
                        ChoiceBatch.empty()
                );

        for (HhQuestionnaireQuestionDto question : command.questions()) {
            chain = chain.thenCompose(batch ->
                    generateWithRetry(
                            command,
                            profile,
                            question,
                            1
                    ).thenApply(batch::append)
            );
        }

        return chain;
    }

    private CompletableFuture<ChoiceResult> generateWithRetry(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> profile,
            HhQuestionnaireQuestionDto question,
            int attempt
    ) {
        log.info(
                "Generating HH questionnaire choice: "
                        + "fieldName={}, attempt={}, optionsCount={}",
                question.fieldName(),
                attempt,
                question.options().size()
        );

        return generateOnce(command, profile, question)
                .exceptionallyCompose(throwable -> {
                    Throwable cause = unwrap(throwable);

                    if (attempt >= MAX_ATTEMPTS) {
                        return CompletableFuture.failedFuture(
                                new QuestionnaireAnswerGenerationUnavailableException(
                                        "LLM could not select a valid "
                                                + "option for questionnaire "
                                                + "field "
                                                + question.fieldName()
                                                + " after "
                                                + MAX_ATTEMPTS
                                                + " attempts",
                                        cause
                                )
                        );
                    }

                    log.warn(
                            "Choice questionnaire generation failed. "
                                    + "Retrying: fieldName={}, "
                                    + "nextAttempt={}, failureType={}, "
                                    + "message={}",
                            question.fieldName(),
                            attempt + 1,
                            cause.getClass().getSimpleName(),
                            normalizedMessage(cause)
                    );

                    return generateWithRetry(
                            command,
                            profile,
                            question,
                            attempt + 1
                    );
                });
    }

    private CompletableFuture<ChoiceResult> generateOnce(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> profile,
            HhQuestionnaireQuestionDto question
    ) {
        LlmGenerationRequest request = new LlmGenerationRequest(
                USE_CASE,
                List.of(
                        LlmMessage.system(
                                promptTemplateRenderer.render(
                                        PromptTemplate
                                                .HH_QUESTIONNAIRE_CHOICE_ANSWER_SYSTEM,
                                        PromptTemplateModel.empty()
                                )
                        ),
                        LlmMessage.user(
                                renderPrompt(
                                        command,
                                        profile,
                                        question
                                )
                        )
                ),
                LlmGenerationOptions.questionnaireChoiceJson()
        );

        return llmPort.generate(request)
                .thenApply(response -> {
                    ChoicePayload payload = parsePayload(
                            response.content()
                    );

                    HhQuestionnaireOptionDto selectedOption =
                            resolveOption(
                                    question,
                                    payload.selectedOptionValue()
                            );

                    String selectionReason = limit(
                            normalize(payload.selectionReason()),
                            MAX_REASON_CHARS
                    );

                    if (selectionReason.isBlank()) {
                        selectionReason =
                                "LLM selected an option from candidate "
                                        + "profile and resume context.";
                    }

                    GeneratedHhQuestionnaireAnswerDto answer =
                            new GeneratedHhQuestionnaireAnswerDto(
                                    question.fieldName(),
                                    selectedOption.label(),
                                    selectedOption.value(),
                                    HhQuestionnaireAnswerQuality.CONFIRMED,
                                    "",
                                    List.of(
                                            "CHOICE:"
                                                    + selectionReason
                                    )
                            );

                    return new ChoiceResult(
                            answer,
                            valueOrDefault(
                                    response.provider(),
                                    "openrouter"
                            ),
                            valueOrDefault(
                                    response.model(),
                                    "unknown"
                            )
                    );
                });
    }

    private String renderPrompt(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> profile,
            HhQuestionnaireQuestionDto question
    ) {
        return promptTemplateRenderer.render(
                PromptTemplate.HH_QUESTIONNAIRE_CHOICE_ANSWER_USER,
                new HhChoiceQuestionPromptContext(
                        buildCandidateProfileFacts(profile),
                        profile.map(
                                        CandidateQuestionnaireProfileDto
                                                ::additionalConfirmedFacts
                                )
                                .filter(value -> !value.isBlank())
                                .orElse("Нет"),
                        limitText(
                                command.resumeText(),
                                MAX_RESUME_CHARS
                        ),
                        question.fieldName(),
                        question.questionText(),
                        question.options()
                )
        );
    }

    private HhQuestionnaireOptionDto resolveOption(
            HhQuestionnaireQuestionDto question,
            String selectedOptionValue
    ) {
        String normalizedValue = normalize(selectedOptionValue);

        if (normalizedValue.isBlank()) {
            throw new IllegalStateException(
                    "LLM did not return selectedOptionValue"
            );
        }

        return question.options()
                .stream()
                .filter(option ->
                        option.value().equals(normalizedValue)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "LLM selected option that is absent "
                                        + "from HH questionnaire: "
                                        + normalizedValue
                        )
                );
    }

    private ChoicePayload parsePayload(String content) {
        String json = extractJson(content);

        try {
            ChoicePayload payload = objectMapper.readValue(
                    json,
                    ChoicePayload.class
            );

            if (payload == null) {
                throw new IllegalStateException(
                        "LLM choice payload is empty"
                );
            }

            return payload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "LLM choice payload is not valid JSON",
                    exception
            );
        }
    }

    private String extractJson(String content) {
        String normalized = normalize(content);

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "LLM choice response must not be blank"
            );
        }

        normalized = normalized.replaceFirst(
                "(?is)^```(?:json)?\\s*",
                ""
        );

        normalized = normalized.replaceFirst(
                "(?is)\\s*```$",
                ""
        ).trim();

        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');

        if (start < 0 || end < start) {
            throw new IllegalStateException(
                    "LLM choice response does not contain JSON object"
            );
        }

        return normalized.substring(start, end + 1);
    }

    private String buildCandidateProfileFacts(
            Optional<CandidateQuestionnaireProfileDto> optionalProfile
    ) {
        if (optionalProfile.isEmpty()) {
            return """
                    Профиль кандидата не сохранён.
                    Подтверждённые личные факты отсутствуют.
                    """;
        }

        CandidateQuestionnaireProfileDto profile =
                optionalProfile.get();

        return """
                Часовой пояс: %s.
                Зарплатные ожидания: %s–%s %s в месяц.
                Основа суммы зарплаты: %s.
                Переезд: %s.
                Предпочтительный формат работы: %s.
                Удалёнка в приоритете: %s.
                Английский: %s.
                Командировки: %s.
                Готовность к тестовому заданию: %s.
                Готовность начать работу: %s.
                """.formatted(
                profile.timeZoneId(),
                formatAmount(profile.salaryMin()),
                formatAmount(profile.salaryMax()),
                profile.salaryCurrency(),
                formatSalaryTaxBasis(profile),
                profile.relocationReady() ? "готов" : "не готов",
                formatWorkFormatPreference(profile),
                profile.remoteWorkPriority() ? "да" : "нет",
                profile.englishLevel(),
                profile.businessTripsReady() ? "готов" : "не готов",
                formatTestAssignmentReadiness(profile),
                formatStartAvailability(profile)
        );
    }

    private String formatAmount(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatSalaryTaxBasis(
            CandidateQuestionnaireProfileDto profile
    ) {
        return switch (profile.salaryTaxBasis()) {
            case NET -> "на руки";
            case GROSS -> "до вычета налогов";
            case UNSPECIFIED -> "не указана";
        };
    }

    private String formatWorkFormatPreference(
            CandidateQuestionnaireProfileDto profile
    ) {
        return switch (profile.workFormatPreference()) {
            case ANY -> "любой";
            case REMOTE -> "удалённый";
            case HYBRID -> "гибридный";
            case OFFICE -> "офис";
        };
    }

    private String formatTestAssignmentReadiness(
            CandidateQuestionnaireProfileDto profile
    ) {
        return switch (profile.testAssignmentReadiness()) {
            case YES -> "готов выполнить";
            case NO -> "не рассматривает";
            case UNKNOWN -> "не указана";
        };
    }

    private String formatStartAvailability(
            CandidateQuestionnaireProfileDto profile
    ) {
        return switch (profile.startAvailability()) {
            case IMMEDIATELY -> "сразу";
            case WITHIN_TWO_WEEKS -> "в течение двух недель";
            case WITHIN_ONE_MONTH -> "в течение месяца";
            case NEGOTIABLE -> "по договорённости";
        };
    }

    private String limitText(
            String value,
            int maxChars
    ) {
        String normalized = normalize(value);

        if (normalized.length() <= maxChars) {
            return normalized;
        }

        return normalized.substring(0, maxChars).strip();
    }

    private String limit(
            String value,
            int maxChars
    ) {
        if (value.length() <= maxChars) {
            return value;
        }

        return value.substring(0, maxChars).strip();
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private String normalizedMessage(Throwable throwable) {
        String message = throwable.getMessage();

        return message == null || message.isBlank()
                ? "No error message"
                : message.trim();
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        String normalized = normalize(value);

        return normalized.isBlank()
                ? defaultValue
                : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record ChoicePayload(
            String selectedOptionValue,
            String selectionReason
    ) {
    }

    private record ChoiceResult(
            GeneratedHhQuestionnaireAnswerDto answer,
            String provider,
            String model
    ) {
    }

    private record ChoiceBatch(
            List<GeneratedHhQuestionnaireAnswerDto> answers,
            String provider,
            String model
    ) {

        private static ChoiceBatch empty() {
            return new ChoiceBatch(
                    List.of(),
                    "",
                    ""
            );
        }

        private ChoiceBatch append(ChoiceResult result) {
            List<GeneratedHhQuestionnaireAnswerDto> updated =
                    new ArrayList<>(answers);

            updated.add(result.answer());

            return new ChoiceBatch(
                    List.copyOf(updated),
                    provider.isBlank()
                            ? result.provider()
                            : provider,
                    model.isBlank()
                            ? result.model()
                            : model
            );
        }

        private String resolvedProvider() {
            return provider.isBlank()
                    ? "openrouter"
                    : provider;
        }

        private String resolvedModel() {
            return model.isBlank()
                    ? "unknown"
                    : model;
        }
    }
}