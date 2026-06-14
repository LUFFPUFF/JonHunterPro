package ru.jobhunter.infrastructure.platform.hh.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HhOAuthCustomSchemeCallbackDispatcherTest {

    private final HhOAuthCustomSchemeCallbackRegistry registry =
            new HhOAuthCustomSchemeCallbackRegistry();

    private final HhOAuthCustomSchemeCallbackDispatcher dispatcher =
            new HhOAuthCustomSchemeCallbackDispatcher(registry);

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }

    @Test
    void shouldIgnoreUnsupportedArgument() {
        boolean dispatched = dispatcher.dispatchIfSupported("--some-regular-argument");

        assertFalse(dispatched);
    }

    @Test
    void shouldDispatchSupportedCallbackUri() throws Exception {
        String expectedState = "dispatcher-state";

        var callbackFuture = registry.waitForCallback(
                expectedState,
                Duration.ofSeconds(5)
        );

        boolean dispatched = dispatcher.dispatchIfSupported(
                "jobhunterpro://oauth/hh/callback?code=dispatcher-code&state=dispatcher-state"
        );

        assertTrue(dispatched);

        HhOAuthCallbackResult result = callbackFuture.get(1, TimeUnit.SECONDS);

        assertEquals("dispatcher-code", result.code());
        assertEquals(expectedState, result.state());
    }

    @Test
    void shouldDispatchOnlySupportedArgumentsFromCollection() throws Exception {
        String expectedState = "collection-state";

        var callbackFuture = registry.waitForCallback(
                expectedState,
                Duration.ofSeconds(5)
        );

        int dispatchedCount = dispatcher.dispatchAll(List.of(
                "--regular",
                "plain-text",
                "jobhunterpro://oauth/hh/callback?code=collection-code&state=collection-state"
        ));

        assertEquals(1, dispatchedCount);

        HhOAuthCallbackResult result = callbackFuture.get(1, TimeUnit.SECONDS);

        assertEquals("collection-code", result.code());
        assertEquals(expectedState, result.state());
    }
}