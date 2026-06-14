package ru.jobhunter.infrastructure.platform.hh.auth;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface HhOAuthCallbackStrategy {

    HhOAuthRedirectMode redirectMode();

    CompletableFuture<HhOAuthCallbackResult> waitForCallback(String expectedState, Duration timeout);
}
