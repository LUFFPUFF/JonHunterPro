package ru.jobhunter.infrastructure.platform.hh.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Set;

@ConfigurationProperties(prefix = "jobhunter.integrations.hh.api")
public record HhApiProperties(
        String baseUrl,
        String userAgent,
        boolean allowNonHhBaseUrl
) {

    private static final String HTTPS_SCHEME = "https";
    private static final String HTTP_SCHEME = "http";
    private static final String HH_API_HOST = "api.hh.ru";

    private static final Set<String> LOOPBACK_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1"
    );

    public HhApiProperties {
        if (isBlank(baseUrl)) {
            throw new HhApiConfigurationException("HH API base URL is not configured");
        }

        if (isBlank(userAgent)) {
            throw new HhApiConfigurationException("HH API User-Agent is not configured");
        }

        baseUrl = normalizeBaseUrl(baseUrl);
        validateBaseUrl(baseUrl, allowNonHhBaseUrl);
        userAgent = userAgent.trim();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value.trim();

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static void validateBaseUrl(String value, boolean allowNonHhBaseUrl) {
        URI uri;

        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new HhApiConfigurationException("HH API base URL is invalid", exception);
        }

        if (allowNonHhBaseUrl) {
            validateDevelopmentBaseUrl(uri);
            return;
        }

        validateProductionBaseUrl(uri);
    }

    private static void validateProductionBaseUrl(URI uri) {
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new HhApiConfigurationException("HH API base URL must use HTTPS");
        }

        if (!HH_API_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new HhApiConfigurationException("HH API base URL must use api.hh.ru domain");
        }
    }

    private static void validateDevelopmentBaseUrl(URI uri) {
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())
                && !HTTP_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new HhApiConfigurationException("HH API base URL must use HTTP or HTTPS");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new HhApiConfigurationException("HH API base URL host is not configured");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}