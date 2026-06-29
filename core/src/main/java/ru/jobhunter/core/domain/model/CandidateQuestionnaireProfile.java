package ru.jobhunter.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class CandidateQuestionnaireProfile {

    private final UserId userId;
    private final CandidateQuestionnaireProfileFacts facts;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CandidateQuestionnaireProfile(
            UserId userId,
            CandidateQuestionnaireProfileFacts facts,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.userId = Objects.requireNonNull(
                userId,
                "User id must not be null"
        );
        this.facts = Objects.requireNonNull(
                facts,
                "Candidate questionnaire profile facts must not be null"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Created at must not be null"
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "Updated at must not be null"
        );

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Updated at must not be before created at"
            );
        }
    }

    public static CandidateQuestionnaireProfile create(
            UserId userId,
            CandidateQuestionnaireProfileFacts facts
    ) {
        Instant now = Instant.now();

        return new CandidateQuestionnaireProfile(
                userId,
                facts,
                now,
                now
        );
    }

    public static CandidateQuestionnaireProfile restore(
            UserId userId,
            CandidateQuestionnaireProfileFacts facts,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new CandidateQuestionnaireProfile(
                userId,
                facts,
                createdAt,
                updatedAt
        );
    }

    public CandidateQuestionnaireProfile changeFacts(
            CandidateQuestionnaireProfileFacts newFacts
    ) {
        return new CandidateQuestionnaireProfile(
                userId,
                newFacts,
                createdAt,
                Instant.now()
        );
    }

    public UserId userId() {
        return userId;
    }

    public CandidateQuestionnaireProfileFacts facts() {
        return facts;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}