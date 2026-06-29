package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhSuitableResumeItemResponse(
        String id,

        @JsonAlias({"title", "name"})
        String title,

        String url
) {
}