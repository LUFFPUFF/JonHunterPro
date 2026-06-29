package ru.jobhunter.core.application.usecase.coverletter;

public record GeneratedCoverLetterDto(
        String vacancyId,
        String vacancyTitle,
        String companyName,
        String content,
        String provider,
        String model
) {

    public GeneratedCoverLetterDto {
        if (vacancyId == null || vacancyId.isBlank()) {
            throw new IllegalArgumentException("Vacancy id must not be blank");
        }

        if (vacancyTitle == null || vacancyTitle.isBlank()) {
            throw new IllegalArgumentException("Vacancy title must not be blank");
        }

        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Generated cover letter content must not be blank");
        }

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("LLM provider must not be blank");
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("LLM model must not be blank");
        }

        vacancyId = vacancyId.trim();
        vacancyTitle = vacancyTitle.trim();
        companyName = companyName.trim();
        content = content.trim();
        provider = provider.trim();
        model = model.trim();
    }
}