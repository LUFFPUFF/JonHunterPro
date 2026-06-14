package ru.jobhunter.infrastructure.platform.hh.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HhOAuthCustomSchemeCallbackRegistryTest {

    private final HhOAuthCustomSchemeCallbackRegistry registry =
            new HhOAuthCustomSchemeCallbackRegistry();

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    void shouldCompleteCallbackWhenStateMatches() throws Exception {
        String expectedState = "expected-state";
        String callbackUri = "jobhunterpro://oauth/hh/callback?code=test-code&state=expected-state";

        var callbackFuture = registry.waitForCallback(
                expectedState,
                Duration.ofSeconds(5)
        );

        boolean completed = registry.completeFromCallbackUri(callbackUri);

        assertTrue(completed);

        HhOAuthCallbackResult result = callbackFuture.get(1, TimeUnit.SECONDS);

        assertEquals("test-code", result.code());
        assertEquals(expectedState, result.state());
    }

    @Test
    void shouldReturnFalseWhenStateIsUnknown() {
        String callbackUri = "jobhunterpro://oauth/hh/callback?code=test-code&state=unknown-state";

        boolean completed = registry.completeFromCallbackUri(callbackUri);

        assertFalse(completed);
    }

    @Test
    void shouldCompleteExceptionallyWhenCallbackTimesOut() {
        String expectedState = "timeout-state";

        var callbackFuture = registry.waitForCallback(
                expectedState,
                Duration.ofMillis(50)
        );

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> callbackFuture.get(1, TimeUnit.SECONDS)
        );

        assertInstanceOf(HhOAuthCallbackException.class, exception.getCause());
        assertEquals(
                "HH OAuth custom URI callback timed out",
                exception.getCause().getMessage()
        );
    }

    @Test
    void shouldCompleteExceptionallyWhenCallbackContainsError() {
        String expectedState = "error-state";
        String callbackUri = "jobhunterpro://oauth/hh/callback?error=access_denied&state=error-state";

        var callbackFuture = registry.waitForCallback(
                expectedState,
                Duration.ofSeconds(5)
        );

        boolean completed = registry.completeFromCallbackUri(callbackUri);

        assertTrue(completed);

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> callbackFuture.get(1, TimeUnit.SECONDS)
        );

        assertInstanceOf(HhOAuthCallbackException.class, exception.getCause());
        assertEquals(
                "HH OAuth authorization failed: access_denied",
                exception.getCause().getMessage()
        );
    }

    @Test
    void shouldRejectCallbackWithUnsupportedScheme() {
        HhOAuthCallbackException exception = assertThrows(
                HhOAuthCallbackException.class,
                () -> registry.completeFromCallbackUri(
                        "https://oauth/hh/callback?code=test-code&state=test-state"
                )
        );

        assertEquals(
                "HH OAuth custom URI callback has unsupported scheme",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectCallbackWithoutCode() {
        HhOAuthCallbackException exception = assertThrows(
                HhOAuthCallbackException.class,
                () -> registry.completeFromCallbackUri(
                        "jobhunterpro://oauth/hh/callback?state=test-state"
                )
        );

        assertEquals(
                "HH OAuth custom URI callback does not contain authorization code",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectCallbackWithoutState() {
        HhOAuthCallbackException exception = assertThrows(
                HhOAuthCallbackException.class,
                () -> registry.completeFromCallbackUri(
                        "jobhunterpro://oauth/hh/callback?code=test-code"
                )
        );

        assertEquals(
                "HH OAuth custom URI callback does not contain state",
                exception.getMessage()
        );
    }
}