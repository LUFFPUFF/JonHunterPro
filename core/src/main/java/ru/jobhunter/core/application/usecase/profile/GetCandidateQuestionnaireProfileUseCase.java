package ru.jobhunter.core.application.usecase.profile;

import ru.jobhunter.core.application.dto.CandidateQuestionnaireProfileDto;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GetCandidateQuestionnaireProfileUseCase {

    CompletableFuture<Optional<CandidateQuestionnaireProfileDto>> findByUserId(
            UserId userId
    );
}