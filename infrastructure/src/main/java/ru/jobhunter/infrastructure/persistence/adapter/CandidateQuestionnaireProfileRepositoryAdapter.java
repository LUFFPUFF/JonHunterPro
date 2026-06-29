package ru.jobhunter.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ru.jobhunter.core.domain.model.CandidateQuestionnaireProfile;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.CandidateQuestionnaireProfileRepository;
import ru.jobhunter.infrastructure.persistence.mapper.CandidateQuestionnaireProfilePersistenceMapper;
import ru.jobhunter.infrastructure.persistence.springdata.SpringDataCandidateQuestionnaireProfileJpaRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Repository
public class CandidateQuestionnaireProfileRepositoryAdapter
        implements CandidateQuestionnaireProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(
            CandidateQuestionnaireProfileRepositoryAdapter.class
    );

    private final SpringDataCandidateQuestionnaireProfileJpaRepository jpaRepository;
    private final CandidateQuestionnaireProfilePersistenceMapper mapper;
    private final Executor executor;

    public CandidateQuestionnaireProfileRepositoryAdapter(
            SpringDataCandidateQuestionnaireProfileJpaRepository jpaRepository,
            CandidateQuestionnaireProfilePersistenceMapper mapper,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<CandidateQuestionnaireProfile> save(
            CandidateQuestionnaireProfile profile
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug(
                    "Saving candidate questionnaire profile: userId={}",
                    profile.userId()
            );

            CandidateQuestionnaireProfile savedProfile = mapper.toDomain(
                    jpaRepository.save(mapper.toEntity(profile))
            );

            log.debug(
                    "Candidate questionnaire profile saved: userId={}",
                    savedProfile.userId()
            );

            return savedProfile;
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<CandidateQuestionnaireProfile>> findByUserId(
            UserId userId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug(
                    "Finding candidate questionnaire profile: userId={}",
                    userId
            );

            return jpaRepository.findById(userId.value())
                    .map(mapper::toDomain);
        }, executor);
    }
}