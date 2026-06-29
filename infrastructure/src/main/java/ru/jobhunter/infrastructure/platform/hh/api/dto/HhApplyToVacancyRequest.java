package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HhApplyToVacancyRequest(
        @JsonProperty("resume_id")
        String resumeId,

        @JsonProperty("vacancy_id")
        String vacancyId,

        String message
) {

    public HhApplyToVacancyRequest {
        resumeId = requireNotBlank(resumeId, "HH resume id must not be blank");
        vacancyId = requireNotBlank(vacancyId, "HH vacancy id must not be blank");
        message = normalize(message);
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}