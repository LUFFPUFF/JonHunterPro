package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class HhOAuthAuthorizationUrlFactory {

    private static final String RESPONSE_TYPE_CODE = "code";
    private static final String HTTPS_HH_HOST = "hh.ru";
    private static final String LOCALHOST = "localhost";
    private static final String LOOPBACK_IPV4 = "127.0.0.1";
    private static final String JOBHUNTER_PROTOCOL = "jobhunterpro";

    private final HhOAuthProperties properties;
    private final HhOAuthStateGenerator stateGenerator;

    public HhOAuthAuthorizationUrlFactory(
            HhOAuthProperties properties,
            HhOAuthStateGenerator stateGenerator
    ) {
        this.properties = Objects.requireNonNull(properties, "HH OAuth properties must not be null");
        this.stateGenerator = Objects.requireNonNull(stateGenerator, "HH OAuth state generator must not be null");
    }

    public HhOAuthAuthorizationUrl createAuthorizationUrl() {
        validateConfiguration();

        String state = stateGenerator.generate();

        String url = properties.authorizationUrl()
                + "?response_type=" + encode(RESPONSE_TYPE_CODE)
                + "&client_id=" + encode(properties.clientId())
                + "&redirect_uri=" + encode(properties.redirectUri())
                + "&state=" + encode(state);

        return new HhOAuthAuthorizationUrl(url, state);
    }

    private void validateConfiguration() {
        validateAuthorizationUrl();
        validateClientId();
        validateRedirectUri();
    }

    private void validateAuthorizationUrl() {
        if (isBlank(properties.authorizationUrl())) {
            throw new HhOAuthConfigurationException("HH authorization URL is not configured");
        }

        URI authorizationUri = URI.create(properties.authorizationUrl());

        if (!"https".equalsIgnoreCase(authorizationUri.getScheme())) {
            throw new HhOAuthConfigurationException("HH authorization URL must use HTTPS");
        }

        if (!HTTPS_HH_HOST.equalsIgnoreCase(authorizationUri.getHost())) {
            throw new HhOAuthConfigurationException("HH authorization URL must use hh.ru domain");
        }
    }

    private void validateClientId() {
        if (isBlank(properties.clientId())) {
            throw new HhOAuthConfigurationException("HH client id is not configured");
        }
    }

    private void validateRedirectUri() {
        if (isBlank(properties.redirectUri())) {
            throw new HhOAuthConfigurationException("HH redirect URI is not configured");
        }

        URI redirectUri = URI.create(properties.redirectUri());
        HhOAuthRedirectMode redirectMode = properties.parsedRedirectMode();

        switch (redirectMode) {
            case LOCAL_HTTP_SERVER -> validateLocalHttpRedirectUri(redirectUri);
            case CUSTOM_URI_SCHEME -> validateCustomSchemeRedirectUri(redirectUri);
        }
    }

    private void validateLocalHttpRedirectUri(URI redirectUri) {
        if (!"http".equalsIgnoreCase(redirectUri.getScheme())) {
            throw new HhOAuthConfigurationException(
                    "HH redirect URI must use HTTP for LOCAL_HTTP_SERVER mode"
            );
        }

        String host = redirectUri.getHost();

        if (!LOOPBACK_IPV4.equals(host) && !LOCALHOST.equalsIgnoreCase(host)) {
            throw new HhOAuthConfigurationException(
                    "HH redirect URI host must be localhost or 127.0.0.1 for LOCAL_HTTP_SERVER mode"
            );
        }

        if (redirectUri.getPort() != properties.callbackPort()) {
            throw new HhOAuthConfigurationException(
                    "HH redirect URI port must match HH callback port"
            );
        }

        if (!"/oauth/hh/callback".equals(redirectUri.getPath())) {
            throw new HhOAuthConfigurationException(
                    "HH redirect URI path must be /oauth/hh/callback"
            );
        }
    }

    private void validateCustomSchemeRedirectUri(URI redirectUri) {
        if (!JOBHUNTER_PROTOCOL.equalsIgnoreCase(redirectUri.getScheme())) {
            throw new HhOAuthConfigurationException(
                    "HH redirect URI must use jobhunterpro scheme for CUSTOM_URI_SCHEME mode"
            );
        }

        if (!"oauth".equalsIgnoreCase(redirectUri.getHost())) {
            throw new HhOAuthConfigurationException(
                    "HH custom redirect URI host must be oauth"
            );
        }

        if (!"/hh/callback".equals(redirectUri.getPath())) {
            throw new HhOAuthConfigurationException(
                    "HH custom redirect URI path must be /hh/callback"
            );
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record HhOAuthAuthorizationUrl(String url, String state) {
    }
}