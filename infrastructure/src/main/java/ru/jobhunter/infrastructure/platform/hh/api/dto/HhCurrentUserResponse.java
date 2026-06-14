package ru.jobhunter.infrastructure.platform.hh.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhCurrentUserResponse(
        String id,
        String email,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        @JsonProperty("middle_name")
        String middleName,

        @JsonProperty("user_type")
        String userType,

        @JsonProperty("is_admin")
        Boolean admin
) {
}
