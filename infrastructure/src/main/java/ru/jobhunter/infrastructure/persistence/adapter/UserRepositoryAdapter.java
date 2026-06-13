package ru.jobhunter.infrastructure.persistence.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.UserRepository;
import ru.jobhunter.infrastructure.persistence.mapper.UserPersistenceMapper;
import ru.jobhunter.infrastructure.persistence.springdata.SpringDataUserJpaRepository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryAdapter.class);

    private final SpringDataUserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;
    private final Executor executor;

    public UserRepositoryAdapter(
            SpringDataUserJpaRepository jpaRepository,
            UserPersistenceMapper mapper,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<User> save(User user) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Saving user: userId={}, email={}", user.id(), user.email());

            var entity = mapper.toEntity(user);
            var savedEntity = jpaRepository.save(entity);

            log.debug("User saved: userId={}, email={}", savedEntity.getId(), savedEntity.getEmail());

            return mapper.toDomain(savedEntity);
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<User>> findById(UserId id) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Finding user by id: userId={}", id);

            return jpaRepository.findById(id.value())
                    .map(mapper::toDomain);
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<User>> findByEmail(Email email) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Finding user by email: email={}", email);

            return jpaRepository.findByEmail(email.value())
                    .map(mapper::toDomain);
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> existsByEmail(Email email) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Checking user existence by email: email={}", email);

            return jpaRepository.existsByEmail(email.value());
        }, executor);
    }
}
