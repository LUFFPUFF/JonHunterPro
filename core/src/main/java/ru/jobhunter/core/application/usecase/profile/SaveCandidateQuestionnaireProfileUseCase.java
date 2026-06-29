package ru.jobhunter.core.application.usecase.profile;

import ru.jobhunter.core.application.dto.CandidateQuestionnaireProfileDto;
import ru.jobhunter.core.application.dto.SaveCandidateQuestionnaireProfileCommand;

import java.util.concurrent.CompletableFuture;

public interface SaveCandidateQuestionnaireProfileUseCase {

    CompletableFuture<CandidateQuestionnaireProfileDto> save(
            SaveCandidateQuestionnaireProfileCommand command
    );
}