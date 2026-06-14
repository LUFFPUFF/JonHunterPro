package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhEmployerShortResponse(
        String id,
        String name,
        String url,

        @JsonProperty("alternate_url")
        String alternateUrl
) {
}
