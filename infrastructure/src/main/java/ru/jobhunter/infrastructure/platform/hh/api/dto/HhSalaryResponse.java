package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhSalaryResponse(
        Integer from,
        Integer to,
        String currency,
        Boolean gross
) {
}
