package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public final class HhOAuthCustomSchemeCallbackWaiter implements HhOAuthCallbackStrategy {

    private final HhOAuthCustomSchemeCallbackRegistry callbackRegistry;

    public HhOAuthCustomSchemeCallbackWaiter(
            HhOAuthCustomSchemeCallbackRegistry callbackRegistry
    ) {
        this.callbackRegistry = Objects.requireNonNull(
                callbackRegistry,
                "HH OAuth custom scheme callback registry must not be null"
        );
    }

    @Override
    public HhOAuthRedirectMode redirectMode() {
        return HhOAuthRedirectMode.CUSTOM_URI_SCHEME;
    }

    @Override
    public CompletableFuture<HhOAuthCallbackResult> waitForCallback(
            String expectedState,
            Duration timeout
    ) {
        Objects.requireNonNull(expectedState, "Expected OAuth state must not be null");
        Objects.requireNonNull(timeout, "OAuth callback timeout must not be null");

        return callbackRegistry.waitForCallback(expectedState, timeout);
    }
}