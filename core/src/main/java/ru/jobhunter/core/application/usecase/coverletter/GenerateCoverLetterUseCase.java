package ru.jobhunter.core.application.usecase.coverletter;

import java.util.concurrent.CompletableFuture;

public interface GenerateCoverLetterUseCase {

    CompletableFuture<GeneratedCoverLetterDto> generate(GenerateCoverLetterCommand command);
}