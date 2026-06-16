package ru.jobhunter.infrastructure.platform.habr.auth;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HabrCareerOAuthAuthorizationUrlFactoryTest {

    @Test
    void shouldCreateAuthorizationUrlWithRequiredParameters() {
        HabrCareerOAuthAuthorizationUrlFactory factory = new HabrCareerOAuthAuthorizationUrlFactory(
                properties("test-client-id", "jobhunterpro://oauth/habr/callback"),
                new HabrCareerOAuthStateGenerator(
                        properties("test-client-id", "jobhunterpro://oauth/habr/callback")
                )
        );

        HabrCareerOAuthAuthorizationUrlFactory.HabrCareerOAuthAuthorizationUrl result =
                factory.createAuthorizationUrl();

        assertNotNull(result);
        assertNotNull(result.url());
        assertNotNull(result.state());
        assertFalse(result.state().isBlank());

        String decodedUrl = URLDecoder.decode(result.url(), StandardCharsets.UTF_8);

        assertTrue(decodedUrl.startsWith("https://career.habr.com/integrations/oauth/authorize?"));
        assertTrue(decodedUrl.contains("response_type=code"));
        assertTrue(decodedUrl.contains("client_id=test-client-id"));
        assertTrue(decodedUrl.contains("redirect_uri=jobhunterpro://oauth/habr/callback"));
        assertTrue(decodedUrl.contains("state=" + result.state()));
    }

    @Test
    void shouldRejectBlankClientId() {
        HabrCareerOAuthConfigurationException exception = assertThrows(
                HabrCareerOAuthConfigurationException.class,
                () -> {
                    HabrCareerOAuthAuthorizationUrlFactory factory = new HabrCareerOAuthAuthorizationUrlFactory(
                            properties(" ", "jobhunterpro://oauth/habr/callback"),
                            new HabrCareerOAuthStateGenerator(
                                    properties("test-client-id", "jobhunterpro://oauth/habr/callback")
                            )
                    );

                    factory.createAuthorizationUrl();
                }
        );

        assertTrue(exception.getMessage().contains("client id"));
    }

    @Test
    void shouldRejectBlankRedirectUri() {
        HabrCareerOAuthConfigurationException exception = assertThrows(
                HabrCareerOAuthConfigurationException.class,
                () -> {
                    HabrCareerOAuthAuthorizationUrlFactory factory = new HabrCareerOAuthAuthorizationUrlFactory(
                            properties("test-client-id", " "),
                            new HabrCareerOAuthStateGenerator(
                                    properties("test-client-id", "jobhunterpro://oauth/habr/callback")
                            )
                    );

                    factory.createAuthorizationUrl();
                }
        );

        assertTrue(exception.getMessage().contains("redirect URI"));
    }

    private HabrCareerOAuthProperties properties(
            String clientId,
            String redirectUri
    ) {
        return new HabrCareerOAuthProperties(
                "https://career.habr.com/integrations/oauth/authorize",
                "https://career.habr.com/integrations/oauth/token",
                clientId,
                "test-client-secret",
                redirectUri,
                false,
                32,
                "JobHunterPro/0.1.0 (test@example.com)"
        );
    }
}