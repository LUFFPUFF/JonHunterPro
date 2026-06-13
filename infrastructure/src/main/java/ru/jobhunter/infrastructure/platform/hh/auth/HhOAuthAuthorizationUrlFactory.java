package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class HhOAuthAuthorizationUrlFactory {

    private static final String RESPONSE_TYPE_CODE = "code";

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
        if (isBlank(properties.authorizationUrl())) {
            throw new HhOAuthConfigurationException("HH authorization URL is not configured");
        }

        if (isBlank(properties.clientId())) {
            throw new HhOAuthConfigurationException("HH client id is not configured");
        }

        if (isBlank(properties.redirectUri())) {
            throw new HhOAuthConfigurationException("HH redirect URI is not configured");
        }

        if (!properties.authorizationUrl().startsWith("https://hh.ru/")) {
            throw new HhOAuthConfigurationException("HH authorization URL must use hh.ru domain");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Authorization URL result.
     *
     * @param url authorization URL to open in browser
     * @param state OAuth state value to verify callback
     */
    public record HhOAuthAuthorizationUrl(String url, String state) {
    }
}
