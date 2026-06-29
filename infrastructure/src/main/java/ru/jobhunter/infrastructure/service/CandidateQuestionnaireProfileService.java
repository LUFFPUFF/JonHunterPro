package ru.jobhunter.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.CandidateQuestionnaireProfileDto;
import ru.jobhunter.core.application.dto.SaveCandidateQuestionnaireProfileCommand;
import ru.jobhunter.core.application.usecase.profile.GetCandidateQuestionnaireProfileUseCase;
import ru.jobhunter.core.application.usecase.profile.SaveCandidateQuestionnaireProfileUseCase;
import ru.jobhunter.core.domain.model.CandidateQuestionnaireProfile;
import ru.jobhunter.core.domain.model.CandidateQuestionnaireProfileFacts;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.CandidateQuestionnaireProfileRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public final class CandidateQuestionnaireProfileService implements
        GetCandidateQuestionnaireProfileUseCase,
        SaveCandidateQuestionnaireProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(
            CandidateQuestionnaireProfileService.class
    );

    private final CandidateQuestionnaireProfileRepository profileRepository;

    public CandidateQuestionnaireProfileService(
            CandidateQuestionnaireProfileRepository profileRepository
    ) {
        this.profileRepository = Objects.requireNonNull(
                profileRepository,
                "Candidate questionnaire profile repository must not be null"
        );
    }

    @Override
    public CompletableFuture<Optional<CandidateQuestionnaireProfileDto>> findByUserId(
            UserId userId
    ) {
        Objects.requireNonNull(userId, "User id must not be null");

        return profileRepository.findByUserId(userId)
                .thenApply(optionalProfile -> optionalProfile.map(
                        CandidateQuestionnaireProfileDto::from
                ));
    }

    @Override
    public CompletableFuture<CandidateQuestionnaireProfileDto> save(
            SaveCandidateQuestionnaireProfileCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Save candidate questionnaire profile command must not be null"
        );

        CandidateQuestionnaireProfileFacts facts = toFacts(command);

        return profileRepository.findByUserId(command.userId())
                .thenCompose(optionalProfile -> {
                    CandidateQuestionnaireProfile profileToSave = optionalProfile
                            .map(existingProfile -> existingProfile.changeFacts(facts))
                            .orElseGet(() -> CandidateQuestionnaireProfile.create(
                                    command.userId(),
                                    facts
                            ));

                    return profileRepository.save(profileToSave);
                })
                .thenApply(CandidateQuestionnaireProfileDto::from)
                .whenComplete((profile, throwable) -> {
                    if (throwable == null) {
                        log.info(
                                "Candidate questionnaire profile saved: userId={}",
                                profile.userId()
                        );
                        return;
                    }

                    log.warn(
                            "Candidate questionnaire profile save failed: userId={}",
                            command.userId(),
                            throwable
                    );
                });
    }

    private CandidateQuestionnaireProfileFacts toFacts(
            SaveCandidateQuestionnaireProfileCommand command
    ) {
        return new CandidateQuestionnaireProfileFacts(
                command.timeZoneId(),
                command.salaryMin(),
                command.salaryMax(),
                command.salaryCurrency(),
                command.salaryTaxBasis(),
                command.relocationReady(),
                command.workFormatPreference(),
                command.remoteWorkPriority(),
                command.englishLevel(),
                command.businessTripsReady(),
                command.testAssignmentReadiness(),
                command.startAvailability(),
                command.allowRelatedExperienceDrafts(),
                command.additionalConfirmedFacts()
        );
    }
}