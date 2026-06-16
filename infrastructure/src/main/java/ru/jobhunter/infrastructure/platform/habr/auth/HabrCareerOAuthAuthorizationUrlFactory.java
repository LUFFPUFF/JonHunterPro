package ru.jobhunter.infrastructure.platform.habr.auth;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class HabrCareerOAuthAuthorizationUrlFactory {

    private static final String RESPONSE_TYPE_CODE = "code";

    private final HabrCareerOAuthProperties properties;
    private final HabrCareerOAuthStateGenerator stateGenerator;

    public HabrCareerOAuthAuthorizationUrlFactory(
            HabrCareerOAuthProperties properties,
            HabrCareerOAuthStateGenerator stateGenerator
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "Habr Career OAuth properties must not be null"
        );
        this.stateGenerator = Objects.requireNonNull(
                stateGenerator,
                "Habr Career OAuth state generator must not be null"
        );
    }

    public HabrCareerOAuthAuthorizationUrl createAuthorizationUrl() {
        validateConfiguration();

        String state = stateGenerator.generate();

        String url = properties.authorizationUrl()
                + "?response_type=" + encode(RESPONSE_TYPE_CODE)
                + "&client_id=" + encode(properties.clientId())
                + "&redirect_uri=" + encode(properties.redirectUri())
                + "&state=" + encode(state);

        return new HabrCareerOAuthAuthorizationUrl(url, state);
    }

    private void validateConfiguration() {
        if (isBlank(properties.authorizationUrl())) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth authorization URL is not configured"
            );
        }

        if (isBlank(properties.clientId())) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth client id is not configured"
            );
        }

        if (isBlank(properties.redirectUri())) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth redirect URI is not configured"
            );
        }

        if (!properties.authorizationUrl().startsWith("https://career.habr.com/")) {
            throw new HabrCareerOAuthConfigurationException(
                    "Habr Career OAuth authorization URL must use career.habr.com domain"
            );
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record HabrCareerOAuthAuthorizationUrl(
            String url,
            String state
    ) {
    }
}
