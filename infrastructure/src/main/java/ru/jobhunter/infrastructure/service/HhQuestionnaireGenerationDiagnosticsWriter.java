package ru.jobhunter.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswersDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class HhQuestionnaireGenerationDiagnosticsWriter {

    private static final Logger log = LoggerFactory.getLogger(
            HhQuestionnaireGenerationDiagnosticsWriter.class
    );

    private final ObjectMapper objectMapper;

    public HhQuestionnaireGenerationDiagnosticsWriter(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
    }

    public DiagnosticRun begin(
            String vacancyId,
            List<HhQuestionnaireQuestionDto> questions,
            String systemPrompt,
            String userPrompt,
            LlmGenerationOptions options
    ) {
        String normalizedVacancyId = requireNotBlank(
                vacancyId,
                "Vacancy id must not be blank"
        );

        Objects.requireNonNull(
                questions,
                "Questionnaire questions must not be null"
        );

        String diagnosticRunId = Instant.now()
                .toString()
                .replace(":", "-")
                .replace(".", "-")
                + "-"
                + UUID.randomUUID();

        Path runDirectory = Path.of(
                "logs",
                "hh-questionnaire-llm-debug",
                safePathSegment(normalizedVacancyId),
                diagnosticRunId
        ).toAbsolutePath().normalize();

        DiagnosticRun run = new DiagnosticRun(
                diagnosticRunId,
                runDirectory
        );

        try {
            Files.createDirectories(runDirectory);

            writeJson(
                    run,
                    "questionnaire-form-snapshot.json",
                    new QuestionnaireFormSnapshot(
                            Instant.now().toString(),
                            normalizedVacancyId,
                            toQuestionSnapshots(questions)
                    )
            );

            writeText(
                    run,
                    "questionnaire-rendered-system-prompt.txt",
                    systemPrompt
            );

            writeText(
                    run,
                    "questionnaire-rendered-user-prompt.txt",
                    userPrompt
            );

            writeJson(
                    run,
                    "questionnaire-llm-request-options.json",
                    new RequestOptionsSnapshot(
                            options.temperature(),
                            options.maxTokens(),
                            options.responseFormat().name(),
                            options.expectedJsonArrayItems()
                    )
            );

            log.info(
                    "HH questionnaire LLM diagnostics started: "
                            + "diagnosticRunId={}, directory={}",
                    run.id(),
                    run.directory()
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not initialize HH questionnaire LLM diagnostics: "
                            + "diagnosticRunId={}",
                    run.id(),
                    exception
            );
        }

        return run;
    }

    public void saveFinalResponse(
            DiagnosticRun run,
            LlmGenerationResponse response
    ) {
        Objects.requireNonNull(run, "Diagnostic run must not be null");
        Objects.requireNonNull(response, "LLM response must not be null");

        writeJson(
                run,
                "questionnaire-llm-final-response.json",
                new FinalResponseSnapshot(
                        Instant.now().toString(),
                        response.provider(),
                        response.model(),
                        response.content(),
                        response.usage()
                )
        );
    }

    public void saveParsedAnswerPlan(
            DiagnosticRun run,
            GeneratedHhQuestionnaireAnswersDto answers
    ) {
        Objects.requireNonNull(run, "Diagnostic run must not be null");
        Objects.requireNonNull(
                answers,
                "Generated questionnaire answers must not be null"
        );

        writeJson(
                run,
                "questionnaire-parsed-answer-plan.json",
                answers
        );
    }

    public void saveFailure(
            DiagnosticRun run,
            Throwable throwable
    ) {
        Objects.requireNonNull(run, "Diagnostic run must not be null");

        Throwable cause = throwable == null
                ? new IllegalStateException("Unknown generation failure")
                : throwable;

        writeJson(
                run,
                "questionnaire-generation-failure.json",
                new FailureSnapshot(
                        Instant.now().toString(),
                        cause.getClass().getName(),
                        normalize(cause.getMessage())
                )
        );
    }

    private List<QuestionSnapshot> toQuestionSnapshots(
            List<HhQuestionnaireQuestionDto> questions
    ) {
        List<QuestionSnapshot> result = new ArrayList<>();

        for (int questionIndex = 0;
             questionIndex < questions.size();
             questionIndex++) {
            HhQuestionnaireQuestionDto question =
                    questions.get(questionIndex);

            result.add(
                    new QuestionSnapshot(
                            questionIndex + 1,
                            question.questionText(),
                            question.fieldType().name(),
                            resolveOtherOptionIndex(question),
                            toOptionSnapshots(question.options())
                    )
            );
        }

        return List.copyOf(result);
    }

    private List<OptionSnapshot> toOptionSnapshots(
            List<HhQuestionnaireOptionDto> options
    ) {
        List<OptionSnapshot> result = new ArrayList<>();

        for (int optionIndex = 0;
             optionIndex < options.size();
             optionIndex++) {
            HhQuestionnaireOptionDto option = options.get(optionIndex);

            result.add(
                    new OptionSnapshot(
                            optionIndex + 1,
                            option.label()
                    )
            );
        }

        return List.copyOf(result);
    }

    private int resolveOtherOptionIndex(
            HhQuestionnaireQuestionDto question
    ) {
        if (!question.isRadioWithOtherText()) {
            return 0;
        }

        for (int optionIndex = 0;
             optionIndex < question.options().size();
             optionIndex++) {
            HhQuestionnaireOptionDto option =
                    question.options().get(optionIndex);

            if (option.value().equals(question.otherOptionValue())) {
                return optionIndex + 1;
            }
        }

        return 0;
    }

    private void writeText(
            DiagnosticRun run,
            String fileName,
            String content
    ) {
        try {
            Files.writeString(
                    run.directory().resolve(fileName),
                    content == null ? "" : content,
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save HH questionnaire text diagnostic: "
                            + "diagnosticRunId={}, file={}",
                    run.id(),
                    fileName,
                    exception
            );
        }
    }

    private void writeJson(
            DiagnosticRun run,
            String fileName,
            Object value
    ) {
        try {
            Files.writeString(
                    run.directory().resolve(fileName),
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(value),
                    StandardCharsets.UTF_8
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not save HH questionnaire JSON diagnostic: "
                            + "diagnosticRunId={}, file={}",
                    run.id(),
                    fileName,
                    exception
            );
        }
    }

    private String requireNotBlank(
            String value,
            String message
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String safePathSegment(
            String value
    ) {
        String normalized = normalize(value)
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        return normalized.isBlank()
                ? "unknown"
                : normalized;
    }

    private String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }

    public record DiagnosticRun(
            String id,
            Path directory
    ) {
    }

    private record QuestionnaireFormSnapshot(
            String createdAt,
            String vacancyId,
            List<QuestionSnapshot> questions
    ) {
    }

    private record QuestionSnapshot(
            int questionIndex,
            String questionText,
            String fieldType,
            int otherOptionIndex,
            List<OptionSnapshot> options
    ) {
    }

    private record OptionSnapshot(
            int optionIndex,
            String label
    ) {
    }

    private record RequestOptionsSnapshot(
            double temperature,
            int maxTokens,
            String responseFormat,
            int expectedJsonArrayItems
    ) {
    }

    private record FinalResponseSnapshot(
            String receivedAt,
            String provider,
            String model,
            String content,
            Object usage
    ) {
    }

    private record FailureSnapshot(
            String failedAt,
            String exceptionType,
            String message
    ) {
    }
}