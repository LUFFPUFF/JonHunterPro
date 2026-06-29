package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.PrimaryResumeContentDto;
import ru.jobhunter.core.application.dto.ResumeDto;
import ru.jobhunter.core.application.usecase.resume.GetPrimaryResumeContentUseCase;
import ru.jobhunter.core.application.usecase.resume.GetPrimaryResumeUseCase;
import ru.jobhunter.core.application.usecase.resume.SavePrimaryResumeUseCase;
import ru.jobhunter.core.domain.model.Resume;
import ru.jobhunter.core.domain.model.ResumeSourceType;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.ResumeRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public final class ResumeService implements
        SavePrimaryResumeUseCase,
        GetPrimaryResumeUseCase,
        GetPrimaryResumeContentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;

    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = Objects.requireNonNull(
                resumeRepository,
                "Resume repository must not be null"
        );
    }

    @Override
    public CompletableFuture<ResumeDto> savePrimaryResume(
            UserId userId,
            String title,
            ResumeSourceType sourceType,
            String originalFileName,
            String content
    ) {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(sourceType, "Resume source type must not be null");

        Resume resume = Resume.createPrimary(
                userId,
                title,
                sourceType,
                originalFileName,
                content
        );

        log.info(
                "Saving primary resume: userId={}, sourceType={}, originalFileProvided={}",
                userId,
                sourceType,
                originalFileName != null && !originalFileName.isBlank()
        );

        return resumeRepository.replacePrimaryResume(resume)
                .thenApply(this::toDto);
    }

    @Override
    public CompletableFuture<Optional<ResumeDto>> getPrimaryResume(UserId userId) {
        Objects.requireNonNull(userId, "User id must not be null");

        return resumeRepository.findPrimaryByUserId(userId)
                .thenApply(optionalResume -> optionalResume.map(this::toDto));
    }

    @Override
    public CompletableFuture<Optional<PrimaryResumeContentDto>> getPrimaryResumeContent(
            UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return resumeRepository.findPrimaryByUserId(userId)
                .thenApply(optionalResume ->
                        optionalResume.map(this::toContentDto)
                );
    }

    private ResumeDto toDto(Resume resume) {
        return new ResumeDto(
                resume.id().value(),
                resume.userId().value(),
                resume.title(),
                resume.sourceType(),
                resume.originalFileName(),
                resume.primary(),
                resume.createdAt(),
                resume.updatedAt()
        );
    }

    private PrimaryResumeContentDto toContentDto(Resume resume) {
        return new PrimaryResumeContentDto(
                resume.id().value(),
                resume.userId().value(),
                resume.title(),
                resume.content()
        );
    }
}