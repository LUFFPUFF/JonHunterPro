package ru.jobhunter.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ru.jobhunter.core.domain.model.GeneralCoverLetterSettings;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.GeneralCoverLetterSettingsRepository;
import ru.jobhunter.infrastructure.persistence.mapper.GeneralCoverLetterSettingsPersistenceMapper;
import ru.jobhunter.infrastructure.persistence.springdata.SpringDataGeneralCoverLetterSettingsJpaRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Repository
public class GeneralCoverLetterSettingsRepositoryAdapter implements GeneralCoverLetterSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(
            GeneralCoverLetterSettingsRepositoryAdapter.class
    );

    private final SpringDataGeneralCoverLetterSettingsJpaRepository jpaRepository;
    private final GeneralCoverLetterSettingsPersistenceMapper mapper;
    private final Executor executor;

    public GeneralCoverLetterSettingsRepositoryAdapter(
            SpringDataGeneralCoverLetterSettingsJpaRepository jpaRepository,
            GeneralCoverLetterSettingsPersistenceMapper mapper,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.jpaRepository = Objects.requireNonNull(
                jpaRepository,
                "General cover letter settings JPA repository must not be null"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "General cover letter settings mapper must not be null"
        );
        this.executor = Objects.requireNonNull(
                executor,
                "Application task executor must not be null"
        );
    }

    @Override
    public CompletableFuture<GeneralCoverLetterSettings> save(
            GeneralCoverLetterSettings settings
    ) {
        Objects.requireNonNull(
                settings,
                "General cover letter settings must not be null"
        );

        return CompletableFuture.supplyAsync(() -> {
            log.debug(
                    "Saving general cover letter settings: userId={}, fallbackEnabled={}",
                    settings.userId(),
                    settings.useWhenLlmUnavailable()
            );

            return mapper.toDomain(
                    jpaRepository.saveAndFlush(
                            mapper.toEntity(settings)
                    )
            );
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<GeneralCoverLetterSettings>> findByUserId(
            UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return CompletableFuture.supplyAsync(() -> {
            log.debug(
                    "Finding general cover letter settings: userId={}",
                    userId
            );

            return jpaRepository.findById(userId.value())
                    .map(mapper::toDomain);
        }, executor);
    }
}