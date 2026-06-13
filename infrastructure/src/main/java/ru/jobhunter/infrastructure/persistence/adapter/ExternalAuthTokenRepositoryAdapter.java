package ru.jobhunter.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ExternalAuthTokenRepository;
import ru.jobhunter.infrastructure.persistence.mapper.ExternalAuthTokenPersistenceMapper;
import ru.jobhunter.infrastructure.persistence.springdata.SpringDataExternalAuthTokenJpaRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Repository
public class ExternalAuthTokenRepositoryAdapter implements ExternalAuthTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(ExternalAuthTokenRepositoryAdapter.class);

    private final SpringDataExternalAuthTokenJpaRepository jpaRepository;
    private final ExternalAuthTokenPersistenceMapper mapper;
    private final TransactionTemplate transactionTemplate;
    private final Executor executor;

    public ExternalAuthTokenRepositoryAdapter(
            SpringDataExternalAuthTokenJpaRepository jpaRepository,
            ExternalAuthTokenPersistenceMapper mapper,
            TransactionTemplate transactionTemplate,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.transactionTemplate = transactionTemplate;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ExternalAuthToken> save(ExternalAuthToken token) {
        return CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> {
            log.debug(
                    "Saving external auth token metadata: userId={}, provider={}, expiresAt={}",
                    token.userId(),
                    token.provider(),
                    token.expiresAt()
            );

            var entity = mapper.toEntity(token);

            jpaRepository.findByUserIdAndProvider(
                    token.userId().value(),
                    token.provider().code()
            ).ifPresent(existingEntity -> entity.setId(existingEntity.getId()));

            var savedEntity = jpaRepository.save(entity);

            log.debug(
                    "External auth token metadata saved: tokenId={}, userId={}, provider={}",
                    savedEntity.getId(),
                    savedEntity.getUserId(),
                    savedEntity.getProvider()
            );

            return mapper.toDomain(savedEntity);
        }), executor);
    }

    @Override
    public CompletableFuture<Optional<ExternalAuthToken>> findByUserIdAndProvider(
            UserId userId,
            AuthProvider provider
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Finding external auth token metadata: userId={}, provider={}", userId, provider);

            return jpaRepository.findByUserIdAndProvider(userId.value(), provider.code())
                    .map(mapper::toDomain);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteByUserIdAndProvider(
            UserId userId,
            AuthProvider provider
    ) {
        return CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            log.debug("Deleting external auth token metadata: userId={}, provider={}", userId, provider);
            jpaRepository.deleteByUserIdAndProvider(userId.value(), provider.code());
        }), executor);
    }
}
