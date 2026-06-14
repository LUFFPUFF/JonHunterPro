package ru.jobhunter.infrastructure.platform.hh.auth;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class HhOAuthAuthorizationUrlFactoryTest {

    @Test
    void shouldCreateAuthorizationUrl() {
        HhOAuthAuthorizationUrlFactory factory = getHhOAuthAuthorizationUrlFactory("test-client-id");

        HhOAuthAuthorizationUrlFactory.HhOAuthAuthorizationUrl result = factory.createAuthorizationUrl();

        assertThat(result.state()).isNotBlank();
        assertThat(result.url()).startsWith("https://hh.ru/oauth/authorize?");
        assertThat(result.url()).contains("response_type=code");
        assertThat(result.url()).contains("client_id=test-client-id");
        assertThat(result.url()).contains("redirect_uri=http%3A%2F%2Flocalhost%3A54345%2Foauth%2Fhh%2Fcallback");
        assertThat(result.url()).contains("state=");
    }

    private static @NonNull HhOAuthAuthorizationUrlFactory getHhOAuthAuthorizationUrlFactory(String clientId) {
        HhOAuthProperties properties = new HhOAuthProperties(
                "https://hh.ru/oauth/authorize",
                "https://api.hh.ru/token",
                clientId,
                "test-client-secret",
                "http://localhost:54345/oauth/hh/callback",
                HhOAuthRedirectMode.LOCAL_HTTP_SERVER.name(),
                54345,
                32,
                "JobHunterPro/0.1.0 (test@example.com)"
        );

        return new HhOAuthAuthorizationUrlFactory(
                properties,
                new HhOAuthStateGenerator(properties)
        );
    }

    @Test
    void shouldFailWhenClientIdIsBlank() {
        HhOAuthAuthorizationUrlFactory factory = getHhOAuthAuthorizationUrlFactory("");

        assertThatThrownBy(factory::createAuthorizationUrl)
                .isInstanceOf(HhOAuthConfigurationException.class)
                .hasMessageContaining("client id");
    }
}