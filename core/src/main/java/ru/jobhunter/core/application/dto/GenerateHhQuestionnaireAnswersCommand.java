package ru.jobhunter.core.application.dto;

import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;

import java.util.List;
import java.util.Objects;

public record GenerateHhQuestionnaireAnswersCommand(UserId userId, VacancySource vacancySource, String vacancyId,
                                                    String vacancyTitle, String companyName, String vacancyDescription,
                                                    String resumeText, List<HhQuestionnaireQuestionDto> questions) {
    public GenerateHhQuestionnaireAnswersCommand {
        Objects.requireNonNull(userId, "User id must not be null");
        Objects.requireNonNull(vacancySource, "Vacancy source must not be null");
        Objects.requireNonNull(questions, "Questionnaire questions must not be null");
        vacancyId = requireNotBlank(vacancyId, "Vacancy id must not be blank");
        vacancyTitle = requireNotBlank(vacancyTitle, "Vacancy title must not be blank");
        companyName = requireNotBlank(companyName, "Company name must not be blank");
        vacancyDescription = requireNotBlank(vacancyDescription, "Vacancy description must not be blank");
        resumeText = requireNotBlank(resumeText, "Resume text must not be blank");
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Questionnaire questions must not be empty");
        }
        questions = List.copyOf(questions);
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
