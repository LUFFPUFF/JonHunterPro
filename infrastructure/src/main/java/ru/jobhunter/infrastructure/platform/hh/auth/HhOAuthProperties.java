package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobhunter.integrations.hh.oauth")
public record HhOAuthProperties(
        String authorizationUrl,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        String redirectMode,
        int callbackPort,
        int stateByteLength,
        String userAgent
) {

    public HhOAuthRedirectMode parsedRedirectMode() {
        return HhOAuthRedirectMode.from(redirectMode);
    }
}
