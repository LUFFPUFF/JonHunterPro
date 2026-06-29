package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import ru.jobhunter.core.application.dto.GenerateHhQuestionnaireAnswersCommand;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;
import ru.jobhunter.core.application.dto.HhVacancyDetailsDto;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.List;
import java.util.Objects;

public record HhQuestionnaireGenerationContext(UserId userId, VacancySource vacancySource, HhVacancyDetailsDto vacancy,
                                               String resumeText) {
    public HhQuestionnaireGenerationContext {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(vacancySource, "Vacancy source must not be null");
        Objects.requireNonNull(vacancy, "Vacancy details must not be null");
        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("Resume text must not be blank");
        }
        resumeText = resumeText.trim();
    }

    public GenerateHhQuestionnaireAnswersCommand toCommand(List<HhQuestionnaireQuestionDto> questions) {
        return new GenerateHhQuestionnaireAnswersCommand(userId, vacancySource, vacancy.externalId(), vacancy.name(), valueOrDefault(vacancy.employerName(), "не указана"), valueOrDefault(vacancy.description(), "Описание вакансии на HH.ru не указано."), resumeText, questions);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
