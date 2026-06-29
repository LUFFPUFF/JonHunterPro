package ru.jobhunter.infrastructure.platform.hh.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobhunter.integrations.hh.application")
public record HhApplicationTokenProperties(
        String accessToken
) {

    public HhApplicationTokenProperties {
        accessToken = normalize(accessToken);
    }

    public String requireAccessToken() {
        if (accessToken == null) {
            throw new HhApiConfigurationException(
                    "HH application access token is not configured. Set HH_APPLICATION_ACCESS_TOKEN."
            );
        }

        return accessToken;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
