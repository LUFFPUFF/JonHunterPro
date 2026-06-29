package ru.jobhunter.infrastructure.prompt;

import java.util.Set;

public enum PromptTemplate {

    COVER_LETTER_SYSTEM(
            "prompts/cover-letter/system.j2",
            Set.of()
    ),

    COVER_LETTER_USER(
            "prompts/cover-letter/user.j2",
            Set.of(
                    "vacancy_source",
                    "vacancy_id",
                    "vacancy_title",
                    "company_name",
                    "vacancy_url",
                    "vacancy_description",
                    "resume_text"
            )
    ),

    COVER_LETTER_REPAIR_SYSTEM(
            "prompts/cover-letter/repair-system.j2",
            Set.of()
    ),

    COVER_LETTER_REPAIR_USER(
            "prompts/cover-letter/repair-user.j2",
            Set.of(
                    "vacancy_source",
                    "vacancy_id",
                    "vacancy_title",
                    "company_name",
                    "vacancy_url",
                    "vacancy_description",
                    "resume_text"
            )
    ),

    HH_QUESTIONNAIRE_SINGLE_ANSWER_SYSTEM(
            "prompts/hh-questionnaire/single-answer-system.j2",
            Set.of()
    ),

    HH_QUESTIONNAIRE_SINGLE_ANSWER_USER(
            "prompts/hh-questionnaire/single-answer-user.j2",
            Set.of(
                    "candidate_profile_facts",
                    "additional_confirmed_facts",
                    "resume_text",
                    "field_name",
                    "question_text"
            )
    ),


    HH_QUESTIONNAIRE_CHOICE_ANSWER_SYSTEM(
            "prompts/hh-questionnaire/choice-answer-system.j2",
            Set.of()
    ),

    HH_QUESTIONNAIRE_CHOICE_ANSWER_USER(
            "prompts/hh-questionnaire/choice-answer-user.j2",
            Set.of(
                    "candidate_profile_facts",
                    "additional_confirmed_facts",
                    "resume_text",
                    "field_name",
                    "question_text",
                    "options"
            )
    ),

    HH_QUESTIONNAIRE_FORM_ANSWER_SYSTEM(
            "prompts/hh-questionnaire/form-answer-system.j2",
            Set.of()
    ),

    HH_QUESTIONNAIRE_FORM_ANSWER_USER(
            "prompts/hh-questionnaire/form-answer-user.j2",
            Set.of(
                    "candidate_profile_facts",
                    "additional_confirmed_facts",
                    "resume_text",
                    "vacancy_title",
                    "company_name",
                    "vacancy_description",
                    "questions"
            )
    );


    private final String resourcePath;
    private final Set<String> requiredVariables;

    PromptTemplate(
            String resourcePath,
            Set<String> requiredVariables
    ) {
        this.resourcePath = resourcePath;
        this.requiredVariables = Set.copyOf(requiredVariables);
    }

    public String resourcePath() {
        return resourcePath;
    }

    public Set<String> requiredVariables() {
        return requiredVariables;
    }
}