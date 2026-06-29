package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationOptions;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmMessage;
import ru.jobhunter.core.application.port.out.llm.LlmPort;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterCommand;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterUseCase;
import ru.jobhunter.core.application.usecase.coverletter.GeneratedCoverLetterDto;
import ru.jobhunter.infrastructure.prompt.CoverLetterPromptContext;
import ru.jobhunter.infrastructure.prompt.PromptTemplate;
import ru.jobhunter.infrastructure.prompt.PromptTemplateModel;
import ru.jobhunter.infrastructure.prompt.PromptTemplateRenderer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class GenerateCoverLetterService
        implements GenerateCoverLetterUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            GenerateCoverLetterService.class
    );

    private static final String USE_CASE = "generate-cover-letter";

    private static final int MAX_VACANCY_DESCRIPTION_CHARS = 8_000;
    private static final int MAX_RESUME_TEXT_CHARS = 8_000;

    private static final int MAX_COVER_LETTER_CHARS = 1_400;
    private static final int MAX_COVER_LETTER_ATTEMPTS = 2;

    private final LlmPort llmPort;
    private final PromptTemplateRenderer promptTemplateRenderer;

    public GenerateCoverLetterService(
            LlmPort llmPort,
            PromptTemplateRenderer promptTemplateRenderer
    ) {
        this.llmPort = Objects.requireNonNull(
                llmPort,
                "LLM port must not be null"
        );
        this.promptTemplateRenderer = Objects.requireNonNull(
                promptTemplateRenderer,
                "Prompt template renderer must not be null"
        );
    }

    @Override
    public CompletableFuture<GeneratedCoverLetterDto> generate(
            GenerateCoverLetterCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Generate cover letter command must not be null"
        );

        log.info(
                "Generating cover letter: userId={}, source={}, "
                        + "vacancyId={}, companyName={}",
                command.userId().value(),
                command.vacancySource(),
                command.vacancyId(),
                command.companyName()
        );

        return generateWithOutputPolicy(
                command,
                1,
                LlmGenerationOptions.coverLetter(),
                PromptTemplate.COVER_LETTER_SYSTEM,
                PromptTemplate.COVER_LETTER_USER,
                Set.of()
        );
    }

    private CompletableFuture<GeneratedCoverLetterDto>
    generateWithOutputPolicy(
            GenerateCoverLetterCommand command,
            int attempt,
            LlmGenerationOptions generationOptions,
            PromptTemplate systemTemplate,
            PromptTemplate userTemplate,
            Set<String> excludedProviderIds
    ) {
        LlmGenerationRequest generationRequest =
                buildGenerationRequest(
                        command,
                        generationOptions,
                        systemTemplate,
                        userTemplate,
                        excludedProviderIds
                );

        return llmPort.generate(generationRequest)
                .thenCompose(response -> {
                    CoverLetterOutputPolicy.ValidationResult validation =
                            CoverLetterOutputPolicy.validate(
                                    response.content(),
                                    MAX_COVER_LETTER_CHARS
                            );

                    if (validation.isAccepted()) {
                        return CompletableFuture.completedFuture(
                                mapToDto(
                                        command,
                                        response,
                                        validation.content()
                                )
                        );
                    }

                    if (attempt >= MAX_COVER_LETTER_ATTEMPTS) {
                        return CompletableFuture.failedFuture(
                                new CoverLetterGenerationUnavailableException(
                                        "LLM did not generate a safe cover "
                                                + "letter after "
                                                + MAX_COVER_LETTER_ATTEMPTS
                                                + " attempts: "
                                                + validation.rejectionReason()
                                )
                        );
                    }

                    LlmGenerationRequest retryRequest =
                            generationRequest.excludingProvider(
                                    response.provider()
                            );

                    log.warn(
                            "Generated cover letter violates output policy. "
                                    + "Retrying with repair prompt and "
                                    + "another provider: vacancyId={}, "
                                    + "attempt={}, rejectedProvider={}, "
                                    + "reason={}, contentLength={}",
                            command.vacancyId(),
                            attempt + 1,
                            response.provider(),
                            validation.rejectionReason(),
                            validation.content().length()
                    );

                    return generateWithOutputPolicy(
                            command,
                            attempt + 1,
                            LlmGenerationOptions.compactCoverLetter(),
                            PromptTemplate.COVER_LETTER_REPAIR_SYSTEM,
                            PromptTemplate.COVER_LETTER_REPAIR_USER,
                            retryRequest.excludedProviderIds()
                    );
                });
    }

    private LlmGenerationRequest buildGenerationRequest(
            GenerateCoverLetterCommand command,
            LlmGenerationOptions generationOptions,
            PromptTemplate systemTemplate,
            PromptTemplate userTemplate,
            Set<String> excludedProviderIds
    ) {
        return new LlmGenerationRequest(
                USE_CASE,
                List.of(
                        LlmMessage.system(
                                promptTemplateRenderer.render(
                                        systemTemplate,
                                        PromptTemplateModel.empty()
                                )
                        ),
                        LlmMessage.user(
                                renderCoverLetterUserPrompt(
                                        command,
                                        userTemplate
                                )
                        )
                ),
                generationOptions,
                excludedProviderIds
        );
    }

    private String renderCoverLetterUserPrompt(
            GenerateCoverLetterCommand command,
            PromptTemplate userTemplate
    ) {
        return promptTemplateRenderer.render(
                userTemplate,
                new CoverLetterPromptContext(
                        command.vacancySource().name(),
                        command.vacancyId(),
                        command.vacancyTitle(),
                        command.companyName(),
                        command.vacancyUrl(),
                        limitText(
                                command.vacancyDescription(),
                                MAX_VACANCY_DESCRIPTION_CHARS
                        ),
                        limitText(
                                command.resumeText(),
                                MAX_RESUME_TEXT_CHARS
                        )
                )
        );
    }

    private GeneratedCoverLetterDto mapToDto(
            GenerateCoverLetterCommand command,
            LlmGenerationResponse response,
            String content
    ) {
        return new GeneratedCoverLetterDto(
                command.vacancyId(),
                command.vacancyTitle(),
                command.companyName(),
                content,
                response.provider(),
                response.model()
        );
    }

    private String limitText(String text, int maxChars) {
        String normalizedText = text.strip();

        if (normalizedText.length() <= maxChars) {
            return normalizedText;
        }

        return normalizedText.substring(0, maxChars).strip()
                + "\n\n[Текст сокращён из-за ограничения размера prompt.]";
    }
}