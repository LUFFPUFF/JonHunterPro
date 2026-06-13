package ru.jobhunter.core.domain.repository;

import ru.jobhunter.core.domain.model.Email;
import ru.jobhunter.core.domain.model.User;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {

    CompletableFuture<User> save(User user);

    CompletableFuture<Optional<User>> findById(UserId id);

    CompletableFuture<Optional<User>> findByEmail(Email email);

    CompletableFuture<Boolean> existsByEmail(Email email);
}
