package ru.jobhunter.core.domain.repository;

import ru.jobhunter.core.domain.model.AuthProvider;
import ru.jobhunter.core.domain.model.ExternalAuthToken;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ExternalAuthTokenRepository {

    CompletableFuture<ExternalAuthToken> save(ExternalAuthToken token);

    CompletableFuture<Optional<ExternalAuthToken>> findByUserIdAndProvider(
            UserId userId,
            AuthProvider provider
    );

    CompletableFuture<Void> deleteByUserIdAndProvider(
            UserId userId,
            AuthProvider provider
    );
}
