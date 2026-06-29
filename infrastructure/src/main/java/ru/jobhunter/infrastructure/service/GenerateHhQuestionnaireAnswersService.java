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
import ru.jobhunter.core.application.usecase.questionnaire.GenerateHhQuestionnaireAnswersUseCase;
import ru.jobhunter.infrastructure.prompt.HhQuestionnaireFormPromptContext;
import ru.jobhunter.infrastructure.prompt.PromptTemplate;
import ru.jobhunter.infrastructure.prompt.PromptTemplateModel;
import ru.jobhunter.infrastructure.prompt.PromptTemplateRenderer;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Service
public class GenerateHhQuestionnaireAnswersService
        implements GenerateHhQuestionnaireAnswersUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            GenerateHhQuestionnaireAnswersService.class
    );

    private static final String USE_CASE =
            "answer-hh-questionnaire-form";

    private static final int MAX_RESUME_CHARS = 3_600;
    private static final int MAX_VACANCY_DESCRIPTION_CHARS = 700;

    private static final int MAX_TEXT_ANSWER_CHARS = 300;
    private static final int MAX_MISSING_FACT_CHARS = 220;

    private static final Set<String>
            PROFILE_DERIVED_TECHNICAL_MARKERS = Set.of(
            "java",
            "spring",
            "spring boot",
            "sql",
            "postgresql",
            "api",
            "rest",
            "rest api",
            "restassured",
            "rest assured",
            "автотест",
            "автоматизац",
            "тестирован",
            "qa",
            "junit",
            "selenium",
            "webdriver",
            "kafka",
            "docker",
            "git",
            "ci/cd",
            "интеграц",
            "микросервис",
            "архитектур",
            "база данных",
            "проектирован"
    );

    private static final Set<String>
            PROFILE_DERIVED_CONFIRMED_FACT_MARKERS = Set.of(
            "зарплат",
            "оклад",
            "доход",
            "на руки",
            "ндфл",
            "финансов",
            "сколько лет",
            "совокупн",
            "стаж",
            "коммерческ",
            "работодател",
            "компани",
            "образован",
            "диплом",
            "военн",
            "гражданств",
            "документ",
            "ип",
            "самозанят",
            "ооо",
            "релокац",
            "переезд"
    );

    private final LlmPort llmPort;
    private final ObjectMapper objectMapper;
    private final GetCandidateQuestionnaireProfileUseCase getCandidateQuestionnaireProfileUseCase;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private final HhQuestionnaireGenerationDiagnosticsWriter diagnosticsWriter;

    public GenerateHhQuestionnaireAnswersService(
            LlmPort llmPort,
            ObjectMapper objectMapper,
            GetCandidateQuestionnaireProfileUseCase getCandidateQuestionnaireProfileUseCase,
            PromptTemplateRenderer promptTemplateRenderer,
            HhQuestionnaireGenerationDiagnosticsWriter diagnosticsWriter
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
                        "Candidate questionnaire profile use case "
                                + "must not be null"
                );
        this.promptTemplateRenderer = Objects.requireNonNull(
                promptTemplateRenderer,
                "Prompt template renderer must not be null"
        );
        this.diagnosticsWriter = Objects.requireNonNull(
                diagnosticsWriter,
                "Questionnaire diagnostics writer must not be null"
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

        validateSupportedQuestions(command.questions());

        return getCandidateQuestionnaireProfileUseCase
                .findByUserId(command.userId())
                .thenCompose(profile -> generateOnce(
                        command,
                        profile
                ))
                .exceptionallyCompose(throwable ->
                        CompletableFuture.failedFuture(
                                toGenerationUnavailableException(
                                        command,
                                        throwable
                                )
                        )
                );
    }

    private QuestionnaireAnswerGenerationUnavailableException
    toGenerationUnavailableException(
            GenerateHhQuestionnaireAnswersCommand command,
            Throwable throwable
    ) {
        Throwable cause = unwrap(throwable);

        if (cause instanceof QuestionnaireAnswerGenerationUnavailableException
                questionnaireException) {
            return questionnaireException;
        }

        log.warn(
                "HH questionnaire form generation failed without retry: "
                        + "vacancyId={}, fieldsCount={}, failureType={}, message={}",
                command.vacancyId(),
                command.questions().size(),
                cause.getClass().getSimpleName(),
                normalize(cause.getMessage())
        );

        return new QuestionnaireAnswerGenerationUnavailableException(
                "LLM could not generate a valid questionnaire form in one attempt",
                cause
        );
    }

    private CompletableFuture<GeneratedHhQuestionnaireAnswersDto>
    generateOnce(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> profile
    ) {
        String systemPrompt = promptTemplateRenderer.render(
                PromptTemplate.HH_QUESTIONNAIRE_FORM_ANSWER_SYSTEM,
                PromptTemplateModel.empty()
        );

        String userPrompt = renderPrompt(
                command,
                profile
        );

        LlmGenerationOptions options =
                LlmGenerationOptions.questionnaireFormJson(
                        command.questions().size()
                );

        HhQuestionnaireGenerationDiagnosticsWriter.DiagnosticRun
                diagnosticRun = diagnosticsWriter.begin(
                String.valueOf(command.vacancyId()),
                command.questions(),
                systemPrompt,
                userPrompt,
                options
        );

        LlmGenerationRequest request = new LlmGenerationRequest(
                USE_CASE,
                List.of(
                        LlmMessage.system(systemPrompt),
                        LlmMessage.user(userPrompt)
                ),
                options
        );

        return llmPort.generate(request)
                .thenApply(response -> {
                    diagnosticsWriter.saveFinalResponse(
                            diagnosticRun,
                            response
                    );

                    log.debug(
                            "HH questionnaire form LLM response received: "
                                    + "provider={}, model={}, contentLength={}, "
                                    + "contentPreview={}",
                            response.provider(),
                            response.model(),
                            normalize(response.content()).length(),
                            previewForLog(response.content())
                    );

                    FormPayload payload = parsePayload(
                            response.content()
                    );

                    List<GeneratedHhQuestionnaireAnswerDto> answers =
                            toGeneratedAnswers(
                                    command.questions(),
                                    payload
                            );

                    GeneratedHhQuestionnaireAnswersDto result =
                            new GeneratedHhQuestionnaireAnswersDto(
                                    answers,
                                    valueOrDefault(
                                            response.provider(),
                                            "unknown"
                                    ),
                                    valueOrDefault(
                                            response.model(),
                                            "unknown"
                                    )
                            );

                    diagnosticsWriter.saveParsedAnswerPlan(
                            diagnosticRun,
                            result
                    );

                    return result;
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        diagnosticsWriter.saveFailure(
                                diagnosticRun,
                                unwrap(throwable)
                        );
                    }
                });
    }

    private String renderPrompt(
            GenerateHhQuestionnaireAnswersCommand command,
            Optional<CandidateQuestionnaireProfileDto> profile
    ) {
        return promptTemplateRenderer.render(
                PromptTemplate.HH_QUESTIONNAIRE_FORM_ANSWER_USER,
                new HhQuestionnaireFormPromptContext(
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
                        command.vacancyTitle(),
                        command.companyName(),
                        limitText(
                                command.vacancyDescription(),
                                MAX_VACANCY_DESCRIPTION_CHARS
                        ),
                        command.questions()
                )
        );
    }

    private GeneratedHhQuestionnaireAnswerDto toGeneratedAnswer(
            int questionIndex,
            HhQuestionnaireQuestionDto question,
            FormAnswerPayload payload
    ) {
        FormAnswerStatus status = requireAnswerStatus(
                payload.status(),
                questionIndex
        );

        String answer = normalize(payload.answer());

        int selectedOptionIndex = requireSelectedOptionIndex(
                payload.selectedOptionIndex(),
                questionIndex
        );

        String missingFact = normalize(payload.missingFact());

        if (status == FormAnswerStatus.CANDIDATE_FACT_REQUIRED) {
            Optional<GeneratedHhQuestionnaireAnswerDto> safeDefault =
                    HhQuestionnaireSafeDefaultAnswerFactory.tryCreate(
                            questionIndex,
                            question
                    );

            if (safeDefault.isPresent()) {
                return safeDefault.get();
            }

            return candidateFactRequiredAnswer(
                    questionIndex,
                    question,
                    answer,
                    selectedOptionIndex,
                    missingFact
            );
        }

        if (status == FormAnswerStatus.PROFILE_DERIVED
                && !isProfileDerivedAllowedQuestion(question)) {
            throw new IllegalStateException(
                    "LLM returned PROFILE_DERIVED for a question that requires "
                            + "a confirmed candidate fact: questionIndex="
                            + questionIndex
            );
        }

        HhQuestionnaireAnswerQuality quality =
                status == FormAnswerStatus.PROFILE_DERIVED
                        ? HhQuestionnaireAnswerQuality.PROFILE_DERIVED
                        : HhQuestionnaireAnswerQuality.CONFIRMED;

        if (!missingFact.isBlank()) {
            throw new IllegalStateException(
                    "LLM returned missingFact for ANSWER status: "
                            + "questionIndex=" + questionIndex
            );
        }

        if (question.isText()) {
            if (answer.isBlank()) {
                throw new IllegalStateException(
                        "LLM returned blank text answer: questionIndex="
                                + questionIndex
                );
            }

            if (selectedOptionIndex != 0) {
                throw new IllegalStateException(
                        "Text questionnaire answer must contain "
                                + "selectedOptionIndex=0: questionIndex="
                                + questionIndex
                );
            }

            answer = shortenTextAnswerIfNeeded(
                    answer,
                    question.fieldName()
            );

            return autoFillAnswer(
                    questionIndex,
                    question.fieldName(),
                    answer,
                    "",
                    quality
            );
        }

        HhQuestionnaireOptionDto selectedOption =
                resolveOptionByIndex(
                        question,
                        questionIndex,
                        selectedOptionIndex
                );

        boolean selectedOtherOption =
                question.isRadioWithOtherText()
                        && selectedOption.value().equals(
                        question.otherOptionValue()
                );

        if (selectedOtherOption && answer.isBlank()) {
            throw new IllegalStateException(
                    "LLM selected other option but did not provide text: "
                            + "questionIndex=" + questionIndex
            );
        }

        if (!selectedOtherOption && !answer.isBlank()) {
            throw new IllegalStateException(
                    "LLM returned text for ordinary radio option: "
                            + "questionIndex=" + questionIndex
            );
        }

        if (selectedOtherOption) {
            answer = shortenTextAnswerIfNeeded(
                    answer,
                    question.otherTextFieldName()
            );
        }

        return autoFillAnswer(
                questionIndex,
                question.fieldName(),
                answer,
                selectedOption.value(),
                quality
        );
    }

    private HhQuestionnaireOptionDto resolveOptionByIndex(
            HhQuestionnaireQuestionDto question,
            int questionIndex,
            int selectedOptionIndex
    ) {
        if (selectedOptionIndex < 1
                || selectedOptionIndex > question.options().size()) {
            throw new IllegalStateException(
                    "LLM selected absent questionnaire option index: "
                            + "questionIndex="
                            + questionIndex
                            + ", selectedOptionIndex="
                            + selectedOptionIndex
            );
        }

        return question.options().get(selectedOptionIndex - 1);
    }

    private GeneratedHhQuestionnaireAnswerDto candidateFactRequiredAnswer(
            int questionIndex,
            HhQuestionnaireQuestionDto question,
            String answer,
            int selectedOptionIndex,
            String missingFact
    ) {
        if (!answer.isBlank()) {
            throw new IllegalStateException(
                    "CANDIDATE_FACT_REQUIRED must not contain answer text: "
                            + "questionIndex=" + questionIndex
            );
        }

        if (selectedOptionIndex != 0) {
            throw new IllegalStateException(
                    "CANDIDATE_FACT_REQUIRED must contain "
                            + "selectedOptionIndex=0: questionIndex="
                            + questionIndex
            );
        }

        String confirmedFact = requireNotBlank(
                missingFact,
                "LLM must name the required candidate fact: questionIndex="
                        + questionIndex
        );

        if (confirmedFact.length() > MAX_MISSING_FACT_CHARS) {
            throw new IllegalStateException(
                    "LLM missing candidate fact description is too long: "
                            + "questionIndex=" + questionIndex
            );
        }

        return new GeneratedHhQuestionnaireAnswerDto(
                question.fieldName(),
                "",
                "",
                HhQuestionnaireAnswerQuality.REVIEW_REQUIRED,
                "Требуется подтверждённый факт кандидата: "
                        + confirmedFact,
                List.of(
                        "LLM_FORM:questionIndex=" + questionIndex,
                        "CANDIDATE_FACT_REQUIRED"
                )
        );
    }

    private FormAnswerStatus requireAnswerStatus(
            String value,
            int questionIndex
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalStateException(
                    "LLM questionnaire answer status is blank: "
                            + "questionIndex=" + questionIndex
            );
        }

        try {
            return FormAnswerStatus.valueOf(
                    normalized.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "LLM returned unsupported questionnaire answer status: "
                            + "questionIndex="
                            + questionIndex
                            + ", status="
                            + normalized,
                    exception
            );
        }
    }

    private List<GeneratedHhQuestionnaireAnswerDto> toGeneratedAnswers(
            List<HhQuestionnaireQuestionDto> questions,
            FormPayload payload
    ) {
        if (payload == null || payload.answers() == null) {
            throw new IllegalStateException(
                    "LLM questionnaire form payload is empty"
            );
        }

        Map<Integer, FormAnswerPayload> answersByIndex =
                new LinkedHashMap<>();

        for (FormAnswerPayload answer : payload.answers()) {
            if (answer == null) {
                throw new IllegalStateException(
                        "LLM questionnaire form contains null answer"
                );
            }

            int questionIndex = requireQuestionIndex(
                    answer.questionIndex()
            );

            if (questionIndex > questions.size()) {
                throw new IllegalStateException(
                        "LLM returned unknown questionnaire questionIndex: "
                                + questionIndex
                );
            }

            FormAnswerPayload previous = answersByIndex.put(
                    questionIndex,
                    answer
            );

            if (previous != null) {
                throw new IllegalStateException(
                        "LLM returned duplicate questionnaire questionIndex: "
                                + questionIndex
                );
            }
        }

        if (answersByIndex.size() != questions.size()) {
            throw new IllegalStateException(
                    "LLM must return an answer for every questionnaire question"
            );
        }

        List<GeneratedHhQuestionnaireAnswerDto> result =
                new ArrayList<>();

        for (int questionIndex = 1;
             questionIndex <= questions.size();
             questionIndex++) {
            HhQuestionnaireQuestionDto question =
                    questions.get(questionIndex - 1);

            FormAnswerPayload payloadAnswer =
                    answersByIndex.remove(questionIndex);

            if (payloadAnswer == null) {
                throw new IllegalStateException(
                        "LLM did not return questionnaire questionIndex: "
                                + questionIndex
                );
            }

            result.add(
                    toGeneratedAnswer(
                            questionIndex,
                            question,
                            payloadAnswer
                    )
            );
        }

        if (!answersByIndex.isEmpty()) {
            throw new IllegalStateException(
                    "LLM returned unknown questionnaire question indexes: "
                            + answersByIndex.keySet()
            );
        }

        return List.copyOf(result);
    }

    private GeneratedHhQuestionnaireAnswerDto autoFillAnswer(
            int questionIndex,
            String fieldName,
            String answer,
            String selectedOptionValue,
            HhQuestionnaireAnswerQuality quality
    ) {
        if (!quality.isAutoFillAllowed()) {
            throw new IllegalArgumentException(
                    "Questionnaire answer quality must allow automatic filling"
            );
        }

        List<String> evidence = new ArrayList<>();
        evidence.add("LLM_FORM:questionIndex=" + questionIndex);

        if (quality == HhQuestionnaireAnswerQuality.PROFILE_DERIVED) {
            evidence.add("PROFILE_DERIVED");
        }

        return new GeneratedHhQuestionnaireAnswerDto(
                fieldName,
                answer,
                selectedOptionValue,
                quality,
                "",
                evidence
        );
    }

    private FormPayload parsePayload(
            String content
    ) {
        String json = HhQuestionnaireJsonEnvelopeNormalizer.normalize(
                content
        );

        try {
            FormPayload payload = objectMapper.readValue(
                    json,
                    FormPayload.class
            );

            if (payload == null) {
                throw new IllegalStateException(
                        "LLM questionnaire form response is empty"
                );
            }

            return payload;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "LLM questionnaire form response is not valid JSON",
                    exception
            );
        }
    }

    private int requireQuestionIndex(
            Integer value
    ) {
        if (value == null || value < 1) {
            throw new IllegalStateException(
                    "LLM questionnaire questionIndex must be positive"
            );
        }

        return value;
    }

    private int requireSelectedOptionIndex(
            Integer value,
            int questionIndex
    ) {
        if (value == null || value < 0) {
            throw new IllegalStateException(
                    "LLM selectedOptionIndex must not be negative: "
                            + "questionIndex=" + questionIndex
            );
        }

        return value;
    }

    private boolean isProfileDerivedAllowedQuestion(
            HhQuestionnaireQuestionDto question
    ) {
        String normalizedQuestion = normalize(
                question.questionText()
        ).toLowerCase(Locale.ROOT);

        if (containsConfirmedFactMarker(normalizedQuestion)) {
            return false;
        }

        if (question.isText()) {
            return true;
        }

        return isProfileDerivedTechnicalQuestion(question);
    }

    private boolean containsConfirmedFactMarker(
            String normalizedQuestion
    ) {
        return PROFILE_DERIVED_CONFIRMED_FACT_MARKERS.stream()
                .anyMatch(normalizedQuestion::contains);
    }

    private boolean isProfileDerivedTechnicalQuestion(
            HhQuestionnaireQuestionDto question
    ) {
        String normalizedQuestion = normalize(
                question.questionText()
        ).toLowerCase(Locale.ROOT);

        return PROFILE_DERIVED_TECHNICAL_MARKERS.stream()
                .anyMatch(normalizedQuestion::contains);
    }

    private void validateSupportedQuestions(
            List<HhQuestionnaireQuestionDto> questions
    ) {
        for (HhQuestionnaireQuestionDto question : questions) {
            if (!question.isText()
                    && !question.isRadio()
                    && !question.isRadioWithOtherText()) {
                throw new IllegalArgumentException(
                        "Unsupported questionnaire field type: "
                                + question.fieldType()
                );
            }
        }
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

    private String formatAmount(
            BigDecimal value
    ) {
        return value.stripTrailingZeros()
                .toPlainString();
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

    private String shortenTextAnswerIfNeeded(
            String answer,
            String fieldName
    ) {
        if (answer.length() <= MAX_TEXT_ANSWER_CHARS) {
            return answer;
        }

        int cutOffIndex = findTextAnswerCutOffIndex(answer);

        if (cutOffIndex <= 0) {
            throw new IllegalStateException(
                    "LLM text answer cannot be shortened without splitting "
                            + "a word: field="
                            + fieldName
            );
        }

        String shortened = answer.substring(0, cutOffIndex)
                .strip();

        if (shortened.isBlank()) {
            throw new IllegalStateException(
                    "LLM text answer became blank after shortening: field="
                            + fieldName
            );
        }

        log.info(
                "HH questionnaire text answer shortened to fit field limit: "
                        + "field={}, originalLength={}, shortenedLength={}",
                fieldName,
                answer.length(),
                shortened.length()
        );

        return shortened;
    }

    private int findTextAnswerCutOffIndex(
            String answer
    ) {
        if (Character.isWhitespace(
                answer.charAt(MAX_TEXT_ANSWER_CHARS)
        ) || isSentenceBoundary(
                answer.charAt(MAX_TEXT_ANSWER_CHARS - 1)
        )) {
            return MAX_TEXT_ANSWER_CHARS;
        }

        int lastBoundaryIndex = 0;

        for (int index = 0;
             index < MAX_TEXT_ANSWER_CHARS;
             index++) {
            char current = answer.charAt(index);

            if (Character.isWhitespace(current)) {
                lastBoundaryIndex = index;
                continue;
            }

            if (isSentenceBoundary(current)) {
                lastBoundaryIndex = index + 1;
            }
        }

        return lastBoundaryIndex;
    }

    private boolean isSentenceBoundary(
            char value
    ) {
        return value == '.'
                || value == '!'
                || value == '?'
                || value == '…';
    }

    private String limitText(
            String value,
            int maxChars
    ) {
        String normalized = normalize(value);

        if (normalized.length() <= maxChars) {
            return normalized;
        }

        return normalized.substring(
                0,
                maxChars
        ).strip();
    }

    private Throwable unwrap(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
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

    private String requireNotBlank(
            String value,
            String message
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalStateException(message);
        }

        return normalized;
    }

    private String previewForLog(
            String value
    ) {
        String normalized = normalize(value)
                .replaceAll("\\s+", " ");

        int maxLength = 500;

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength) + "...";
    }

    private String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    private record FormPayload(
            List<FormAnswerPayload> answers
    ) {
    }

    private record FormAnswerPayload(
            Integer questionIndex,
            String status,
            String answer,
            Integer selectedOptionIndex,
            String missingFact
    ) {
    }

    private enum FormAnswerStatus {
        ANSWER,
        PROFILE_DERIVED,
        CANDIDATE_FACT_REQUIRED
    }
}