package ru.jobhunter.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.jobhunter.core.application.dto.GenerateHhQuestionnaireAnswersCommand;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswerDto;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswersDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireAnswerQuality;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmPort;
import ru.jobhunter.core.application.port.out.llm.LlmUsage;
import ru.jobhunter.core.application.usecase.profile.GetCandidateQuestionnaireProfileUseCase;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.infrastructure.prompt.PromptTemplateRenderer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateHhQuestionnaireAnswersServiceTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private GetCandidateQuestionnaireProfileUseCase profileUseCase;

    @Mock
    private PromptTemplateRenderer promptTemplateRenderer;

    @Mock
    private HhQuestionnaireGenerationDiagnosticsWriter diagnosticsWriter;

    private GenerateHhQuestionnaireAnswersService service;

    @BeforeEach
    void setUp() {
        service = new GenerateHhQuestionnaireAnswersService(
                llmPort,
                new ObjectMapper(),
                profileUseCase,
                promptTemplateRenderer,
                diagnosticsWriter
        );

        when(promptTemplateRenderer.render(any(), any()))
                .thenReturn("rendered prompt");
        when(diagnosticsWriter.begin(
                anyString(),
                anyList(),
                anyString(),
                anyString(),
                any()
        )).thenReturn(
                new HhQuestionnaireGenerationDiagnosticsWriter.DiagnosticRun(
                        "test-run",
                        Path.of("target", "test-diagnostics")
                )
        );
    }

    @Test
    void shouldShortenOverlongTextAnswerAtWordBoundaryInsteadOfFailingGeneration() {
        String originalAnswer = (
                "Мне интересна эта вакансия, потому что она сочетает "
                        + "практические задачи, развитие инженерных навыков "
                        + "и работу в команде. "
        ).repeat(4).strip();

        assertTrue(originalAnswer.length() > 300);

        when(profileUseCase.findByUserId(any(UserId.class)))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(llmPort.generate(any())).thenReturn(
                CompletableFuture.completedFuture(
                        new LlmGenerationResponse(
                                "test-provider",
                                "test-model",
                                """
                                        {
                                          "answers": [
                                            {
                                              "questionIndex": 1,
                                              "status": "PROFILE_DERIVED",
                                              "answer": "%s",
                                              "selectedOptionIndex": 0,
                                              "missingFact": ""
                                            }
                                          ]
                                        }
                                        """.formatted(originalAnswer),
                                LlmUsage.unknown()
                        )
                )
        );

        GeneratedHhQuestionnaireAnswersDto result = assertDoesNotThrow(
                () -> service.generate(command()).join()
        );

        GeneratedHhQuestionnaireAnswerDto answer = result.answers().getFirst();

        assertEquals(
                HhQuestionnaireAnswerQuality.PROFILE_DERIVED,
                answer.quality()
        );
        assertTrue(answer.answer().length() <= 300);
        assertTrue(originalAnswer.startsWith(answer.answer()));
        assertTrue(
                Character.isWhitespace(
                        originalAnswer.charAt(answer.answer().length())
                )
        );
    }

    private GenerateHhQuestionnaireAnswersCommand command() {
        return new GenerateHhQuestionnaireAnswersCommand(
                UserId.of(UUID.fromString(
                        "b7d9e0d1-86cd-486d-a789-aa7d805fe73c"
                )),
                VacancySource.HH_RU,
                "123456",
                "Java Developer",
                "Example Company",
                "Разработка backend-сервисов.",
                "Java, Spring Boot, PostgreSQL.",
                List.of(
                        new HhQuestionnaireQuestionDto(
                                "task_interest",
                                "Почему вам интересна эта вакансия?"
                        )
                )
        );
    }
}
