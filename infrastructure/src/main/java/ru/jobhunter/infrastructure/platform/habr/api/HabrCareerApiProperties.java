package ru.jobhunter.infrastructure.platform.habr.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "jobhunter.integrations.habr-career.api")
public record HabrCareerApiProperties(
        String baseUrl,
        String userAgent,
        boolean allowNonHabrCareerBaseUrl
) {

    private static final String HTTPS_SCHEME = "https";
    private static final String HTTP_SCHEME = "http";
    private static final String HABR_CAREER_HOST = "career.habr.com";

    public HabrCareerApiProperties {
        if (isBlank(baseUrl)) {
            throw new HabrCareerApiConfigurationException("Habr Career API base URL is not configured");
        }

        if (isBlank(userAgent)) {
            throw new HabrCareerApiConfigurationException("Habr Career API User-Agent is not configured");
        }

        baseUrl = normalizeBaseUrl(baseUrl);
        validateBaseUrl(baseUrl, allowNonHabrCareerBaseUrl);
        userAgent = userAgent.trim();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value.trim();

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static void validateBaseUrl(String value, boolean allowNonHabrCareerBaseUrl) {
        URI uri;

        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new HabrCareerApiConfigurationException("Habr Career API base URL is invalid", exception);
        }

        if (allowNonHabrCareerBaseUrl) {
            validateDevelopmentBaseUrl(uri);
            return;
        }

        validateProductionBaseUrl(uri);
    }

    private static void validateProductionBaseUrl(URI uri) {
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new HabrCareerApiConfigurationException("Habr Career API base URL must use HTTPS");
        }

        if (!HABR_CAREER_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new HabrCareerApiConfigurationException("Habr Career API base URL must use career.habr.com domain");
        }
    }

    private static void validateDevelopmentBaseUrl(URI uri) {
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())
                && !HTTP_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new HabrCareerApiConfigurationException("Habr Career API base URL must use HTTP or HTTPS");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new HabrCareerApiConfigurationException("Habr Career API base URL host is not configured");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}