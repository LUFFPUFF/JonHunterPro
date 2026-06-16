package ru.jobhunter.infrastructure.platform.habr.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HabrCareerOAuthStateGeneratorTest {

    @Test
    void shouldGenerateDifferentUrlSafeStates() {
        HabrCareerOAuthStateGenerator generator = new HabrCareerOAuthStateGenerator(
                properties(32)
        );

        String firstState = generator.generate();
        String secondState = generator.generate();

        assertNotNull(firstState);
        assertNotNull(secondState);
        assertNotEquals(firstState, secondState);
        assertFalse(firstState.isBlank());
        assertFalse(secondState.isBlank());
        assertTrue(firstState.matches("^[A-Za-z0-9_-]+$"));
        assertTrue(secondState.matches("^[A-Za-z0-9_-]+$"));
    }

    @Test
    void shouldRejectTooShortStateByteLength() {
        HabrCareerOAuthConfigurationException exception = assertThrows(
                HabrCareerOAuthConfigurationException.class,
                () -> new HabrCareerOAuthStateGenerator(properties(8)).generate()
        );

        assertTrue(exception.getMessage().contains("at least 16"));
    }

    private HabrCareerOAuthProperties properties(int stateByteLength) {
        return new HabrCareerOAuthProperties(
                "https://career.habr.com/integrations/oauth/authorize",
                "https://career.habr.com/integrations/oauth/token",
                "test-client-id",
                "test-client-secret",
                "jobhunterpro://oauth/habr/callback",
                false,
                stateByteLength,
                "JobHunterPro/0.1.0 (test@example.com)"
        );
    }
}