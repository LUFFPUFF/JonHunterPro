package ru.jobhunter.infrastructure.persistence.adapter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import ru.jobhunter.core.domain.model.Resume;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ResumeRepository;
import ru.jobhunter.infrastructure.persistence.entity.ResumeEntity;
import ru.jobhunter.infrastructure.persistence.mapper.ResumePersistenceMapper;
import ru.jobhunter.infrastructure.persistence.springdata.SpringDataResumeJpaRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Repository
public class ResumeRepositoryAdapter implements ResumeRepository {

    private final SpringDataResumeJpaRepository jpaRepository;
    private final ResumePersistenceMapper mapper;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService executorService;

    public ResumeRepositoryAdapter(
            SpringDataResumeJpaRepository jpaRepository,
            ResumePersistenceMapper mapper,
            TransactionTemplate transactionTemplate,
            @Qualifier("applicationTaskExecutor") ExecutorService executorService
    ) {
        this.jpaRepository = Objects.requireNonNull(
                jpaRepository,
                "Resume JPA repository must not be null"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "Resume persistence mapper must not be null"
        );
        this.transactionTemplate = Objects.requireNonNull(
                transactionTemplate,
                "Transaction template must not be null"
        );
        this.executorService = Objects.requireNonNull(
                executorService,
                "Executor service must not be null"
        );
    }

    @Override
    public CompletableFuture<Resume> replacePrimaryResume(Resume resume) {
        Objects.requireNonNull(resume, "Resume must not be null");

        if (!resume.primary()) {
            throw new IllegalArgumentException(
                    "Only a primary resume can replace the current primary resume"
            );
        }

        return CompletableFuture.supplyAsync(
                () -> Objects.requireNonNull(
                        transactionTemplate.execute(transactionStatus -> {
                            jpaRepository.clearPrimaryResumeByUserId(
                                    resume.userId().value(),
                                    Instant.now()
                            );

                            ResumeEntity savedEntity = jpaRepository.saveAndFlush(
                                    mapper.toEntity(resume)
                            );

                            return mapper.toDomain(savedEntity);
                        }),
                        "Resume replacement transaction result must not be null"
                ),
                executorService
        );
    }

    @Override
    public CompletableFuture<Optional<Resume>> findPrimaryByUserId(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return CompletableFuture.supplyAsync(
                () -> jpaRepository.findByUserIdAndPrimaryResumeTrue(userId.value())
                        .map(mapper::toDomain),
                executorService
        );
    }
}