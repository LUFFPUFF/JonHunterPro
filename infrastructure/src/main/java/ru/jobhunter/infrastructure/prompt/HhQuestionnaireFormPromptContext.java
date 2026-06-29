package ru.jobhunter.infrastructure.prompt;

import ru.jobhunter.core.application.dto.HhQuestionnaireOptionDto;
import ru.jobhunter.core.application.dto.HhQuestionnaireQuestionDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record HhQuestionnaireFormPromptContext(
        String candidateProfileFacts,
        String additionalConfirmedFacts,
        String resumeText,
        String vacancyTitle,
        String companyName,
        String vacancyDescription,
        List<HhQuestionnaireQuestionDto> questions
) implements PromptTemplateModel {

    public HhQuestionnaireFormPromptContext {
        candidateProfileFacts = requireNotBlank(
                candidateProfileFacts,
                "Candidate profile facts must not be blank"
        );

        additionalConfirmedFacts = requireNotBlank(
                additionalConfirmedFacts,
                "Additional confirmed facts must not be blank"
        );

        resumeText = requireNotBlank(
                resumeText,
                "Resume text must not be blank"
        );

        vacancyTitle = requireNotBlank(
                vacancyTitle,
                "Vacancy title must not be blank"
        );

        companyName = requireNotBlank(
                companyName,
                "Company name must not be blank"
        );

        vacancyDescription = requireNotBlank(
                vacancyDescription,
                "Vacancy description must not be blank"
        );

        Objects.requireNonNull(
                questions,
                "Questionnaire questions must not be null"
        );

        if (questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Questionnaire questions must not be empty"
            );
        }

        questions = List.copyOf(questions);
    }

    @Override
    public Map<String, Object> toTemplateModel() {
        return Map.of(
                "candidate_profile_facts", candidateProfileFacts,
                "additional_confirmed_facts", additionalConfirmedFacts,
                "resume_text", resumeText,
                "vacancy_title", vacancyTitle,
                "company_name", companyName,
                "vacancy_description", vacancyDescription,
                "questions", toPromptQuestions()
        );
    }

    private List<Map<String, Object>> toPromptQuestions() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int index = 0; index < questions.size(); index++) {
            HhQuestionnaireQuestionDto question = questions.get(index);

            Map<String, Object> promptQuestion = new LinkedHashMap<>();
            promptQuestion.put("questionIndex", index + 1);
            promptQuestion.put("fieldType", question.fieldType().name());
            promptQuestion.put("questionText", question.questionText());
            promptQuestion.put("options", toPromptOptions(question.options()));
            promptQuestion.put(
                    "otherOptionIndex",
                    resolveOtherOptionIndex(question)
            );

            result.add(Map.copyOf(promptQuestion));
        }

        return List.copyOf(result);
    }

    private List<Map<String, Object>> toPromptOptions(
            List<HhQuestionnaireOptionDto> options
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int index = 0; index < options.size(); index++) {
            HhQuestionnaireOptionDto option = options.get(index);

            result.add(
                    Map.of(
                            "optionIndex", index + 1,
                            "label", option.label()
                    )
            );
        }

        return List.copyOf(result);
    }

    private int resolveOtherOptionIndex(
            HhQuestionnaireQuestionDto question
    ) {
        if (!question.isRadioWithOtherText()) {
            return 0;
        }

        for (int index = 0; index < question.options().size(); index++) {
            HhQuestionnaireOptionDto option = question.options().get(index);

            if (option.value().equals(question.otherOptionValue())) {
                return index + 1;
            }
        }

        throw new IllegalStateException(
                "Other option was not found in questionnaire options: "
                        + question.fieldName()
        );
    }

    private static String requireNotBlank(
            String value,
            String message
    ) {
        String normalized = value == null
                ? ""
                : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }
}