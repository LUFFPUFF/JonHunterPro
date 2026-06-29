package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhVacancyDetailsResponse(
        String id,
        String name,
        String description,

        @JsonProperty("alternate_url")
        String alternateUrl,

        HhDictionaryItemResponse area,
        HhEmployerShortResponse employer,

        @JsonProperty("key_skills")
        List<HhDictionaryItemResponse> keySkills,

        HhDictionaryItemResponse experience,
        HhDictionaryItemResponse employment,
        HhDictionaryItemResponse schedule,

        @JsonProperty("response_letter_required")
        Boolean responseLetterRequired
) {
}