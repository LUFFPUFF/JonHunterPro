package ru.jobhunter.infrastructure.platform.habr.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "jobhunter.integrations.habr-career.oauth")
public record HabrCareerOAuthProperties(
        String authorizationUrl,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        boolean allowNonHabrCareerOAuthUrls,
        int stateByteLength,
        String userAgent
) {

    private static final String HTTPS_SCHEME = "https";
    private static final String HTTP_SCHEME = "http";
    private static final String HABR_CAREER_HOST = "career.habr.com";

    public HabrCareerOAuthProperties {
        if (isBlank(authorizationUrl)) {
            throw new HabrCareerOAuthConfigurationException("Habr Career OAuth authorization URL is not configured");
        }

        if (isBlank(tokenUrl)) {
            throw new HabrCareerOAuthConfigurationException("Habr Career OAuth token URL is not configured");
        }

        if (isBlank(redirectUri)) {
            throw new HabrCareerOAuthConfigurationException("Habr Career OAuth redirect URI is not configured");
        }

        if (isBlank(userAgent)) {
            throw new HabrCareerOAuthConfigurationException("Habr Career OAuth User-Agent is not configured");
        }

        authorizationUrl = normalizeUrl(authorizationUrl);
        tokenUrl = normalizeUrl(tokenUrl);
        redirectUri = redirectUri.trim();
        userAgent = userAgent.trim();

        validateUrl(authorizationUrl, "authorization URL", allowNonHabrCareerOAuthUrls);
        validateUrl(tokenUrl, "token URL", allowNonHabrCareerOAuthUrls);

        if (stateByteLength < 16) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth state byte length must be at least 16"
            );
        }
    }

    private static String normalizeUrl(String value) {
        String normalized = value.trim();

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static void validateUrl(
            String value,
            String fieldName,
            boolean allowNonHabrCareerOAuthUrls
    ) {
        URI uri;

        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth " + fieldName + " is invalid",
                    exception
            );
        }

        if (allowNonHabrCareerOAuthUrls) {
            validateDevelopmentUrl(uri, fieldName);
            return;
        }

        validateProductionUrl(uri, fieldName);
    }

    private static void validateProductionUrl(URI uri, String fieldName) {
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth " + fieldName + " must use HTTPS"
            );
        }

        if (!HABR_CAREER_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth " + fieldName + " must use career.habr.com domain"
            );
        }
    }

    private static void validateDevelopmentUrl(URI uri, String fieldName) {
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())
                && !HTTP_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth " + fieldName + " must use HTTP or HTTPS"
            );
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth " + fieldName + " host is not configured"
            );
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}