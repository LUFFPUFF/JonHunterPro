package ru.jobhunter.core.application.usecase.questionnaire;

import ru.jobhunter.core.application.dto.GenerateHhQuestionnaireAnswersCommand;
import ru.jobhunter.core.application.dto.GeneratedHhQuestionnaireAnswersDto;

import java.util.concurrent.CompletableFuture;

public interface GenerateHhQuestionnaireAnswersUseCase {
    CompletableFuture<GeneratedHhQuestionnaireAnswersDto> generate(GenerateHhQuestionnaireAnswersCommand command);
}
