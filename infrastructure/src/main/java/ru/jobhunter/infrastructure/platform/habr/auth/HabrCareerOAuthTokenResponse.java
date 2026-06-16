package ru.jobhunter.infrastructure.platform.habr.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HabrCareerOAuthTokenResponse(
        @JsonProperty("access_token")
        String accessToken
) {

    public HabrCareerOAuthTokenResponse {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Habr Career access token must not be blank");
        }

        accessToken = accessToken.trim();
    }
}