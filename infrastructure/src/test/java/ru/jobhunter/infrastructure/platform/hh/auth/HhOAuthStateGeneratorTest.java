package ru.jobhunter.infrastructure.platform.hh.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class HhOAuthStateGeneratorTest {

    @Test
    void shouldGenerateDifferentUrlSafeStates() {
        HhOAuthProperties properties = new HhOAuthProperties(
                "https://hh.ru/oauth/authorize",
                "https://api.hh.ru/token",
                "client-id",
                "client-secret",
                "http://localhost:54345/oauth/hh/callback",
                HhOAuthRedirectMode.LOCAL_HTTP_SERVER.name(),
                54345,
                32,
                "JobHunterPro/0.1.0 (test@example.com)"
        );

        HhOAuthStateGenerator generator = new HhOAuthStateGenerator(properties);

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("+", "/", "=");
    }
}