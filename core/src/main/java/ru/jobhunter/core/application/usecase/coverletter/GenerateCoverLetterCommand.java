package ru.jobhunter.core.application.usecase.coverletter;

import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.Objects;

public record GenerateCoverLetterCommand(
        UserId userId,
        VacancySource vacancySource,
        String vacancyId,
        String vacancyTitle,
        String companyName,
        String vacancyUrl,
        String vacancyDescription,
        String resumeText
) {

    public GenerateCoverLetterCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(vacancySource, "Vacancy source must not be null");

        if (vacancyId == null || vacancyId.isBlank()) {
            throw new IllegalArgumentException("Vacancy id must not be blank");
        }

        if (vacancyTitle == null || vacancyTitle.isBlank()) {
            throw new IllegalArgumentException("Vacancy title must not be blank");
        }

        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank");
        }

        if (vacancyUrl == null || vacancyUrl.isBlank()) {
            throw new IllegalArgumentException("Vacancy URL must not be blank");
        }

        if (vacancyDescription == null || vacancyDescription.isBlank()) {
            throw new IllegalArgumentException("Vacancy description must not be blank");
        }

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("Resume text must not be blank");
        }

        vacancyId = vacancyId.trim();
        vacancyTitle = vacancyTitle.trim();
        companyName = companyName.trim();
        vacancyUrl = vacancyUrl.trim();
        vacancyDescription = vacancyDescription.trim();
        resumeText = resumeText.trim();
    }
}