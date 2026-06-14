package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhVacancySearchResponse(
        List<HhVacancyItemResponse> items,
        Integer found,
        Integer pages,
        Integer page,

        @JsonProperty("per_page")
        Integer perPage
) {
}
