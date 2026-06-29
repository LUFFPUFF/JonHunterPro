package ru.jobhunter.core.domain.repository;

import ru.jobhunter.core.domain.model.CandidateQuestionnaireProfile;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CandidateQuestionnaireProfileRepository {

    CompletableFuture<CandidateQuestionnaireProfile> save(
            CandidateQuestionnaireProfile profile
    );

    CompletableFuture<Optional<CandidateQuestionnaireProfile>> findByUserId(
            UserId userId
    );
}