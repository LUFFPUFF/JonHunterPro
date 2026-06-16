package ru.jobhunter.infrastructure.platform.habr.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HabrCareerCurrentUserResponse(
        String login,
        String email,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        @JsonProperty("middle_name")
        String middleName,

        String birthday,
        String avatar,
        Location location,
        String gender
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(
            String city,
            String country
    ) {
    }
}