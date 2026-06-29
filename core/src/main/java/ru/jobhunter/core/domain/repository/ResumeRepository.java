package ru.jobhunter.core.domain.repository;

import ru.jobhunter.core.domain.model.Resume;
import ru.jobhunter.core.domain.model.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ResumeRepository {

    CompletableFuture<Resume> replacePrimaryResume(Resume resume);

    CompletableFuture<Optional<Resume>> findPrimaryByUserId(UserId userId);
}