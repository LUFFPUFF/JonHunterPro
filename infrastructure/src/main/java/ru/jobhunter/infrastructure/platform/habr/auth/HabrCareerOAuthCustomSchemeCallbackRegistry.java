package ru.jobhunter.infrastructure.platform.habr.auth;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public final class HabrCareerOAuthCustomSchemeCallbackRegistry {

    private static final Logger log =
            LoggerFactory.getLogger(HabrCareerOAuthCustomSchemeCallbackRegistry.class);

    private static final String SCHEME = "jobhunterpro";
    private static final String HOST = "oauth";
    private static final String PATH = "/habr/callback";

    private final ConcurrentMap<String, CompletableFuture<HabrCareerOAuthCallbackResult>> pendingCallbacks =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService timeoutScheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "habr-career-oauth-custom-scheme-timeout");
                thread.setDaemon(true);
                return thread;
            });

    public CompletableFuture<HabrCareerOAuthCallbackResult> waitForCallback(
            String expectedState,
            Duration timeout
    ) {
        if (expectedState == null || expectedState.isBlank()) {
            throw new HabrCareerOAuthCallbackException("Expected Habr Career OAuth state must not be blank");
        }

        Objects.requireNonNull(timeout, "Habr Career OAuth callback timeout must not be null");

        if (timeout.isZero() || timeout.isNegative()) {
            throw new HabrCareerOAuthCallbackException("Habr Career OAuth callback timeout must be positive");
        }

        CompletableFuture<HabrCareerOAuthCallbackResult> callbackFuture = new CompletableFuture<>();

        CompletableFuture<HabrCareerOAuthCallbackResult> previous =
                pendingCallbacks.putIfAbsent(expectedState, callbackFuture);

        if (previous != null) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback is already pending for the same state"
            );
        }

        var timeoutTask = timeoutScheduler.schedule(
                () -> completeTimeout(expectedState),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        callbackFuture.whenComplete((result, throwable) -> {
            pendingCallbacks.remove(expectedState);
            timeoutTask.cancel(false);
        });

        log.info("Habr Career OAuth custom URI callback waiting started");

        return callbackFuture;
    }

    public boolean completeFromCallbackUri(String rawCallbackUri) {
        ParsedCustomSchemeCallback callback = parseCallbackUri(rawCallbackUri);

        if (callback.error() != null) {
            return completeExceptionally(
                    callback.state(),
                    "Habr Career OAuth authorization failed: " + callback.error()
            );
        }

        CompletableFuture<HabrCareerOAuthCallbackResult> callbackFuture =
                pendingCallbacks.remove(callback.state());

        if (callbackFuture == null) {
            log.warn("Habr Career OAuth custom URI callback received, but pending state was not found");
            return false;
        }

        callbackFuture.complete(new HabrCareerOAuthCallbackResult(
                callback.code(),
                callback.state()
        ));

        log.info("Habr Career OAuth custom URI callback completed successfully");

        return true;
    }

    private void completeTimeout(String expectedState) {
        CompletableFuture<HabrCareerOAuthCallbackResult> callbackFuture =
                pendingCallbacks.remove(expectedState);

        if (callbackFuture == null) {
            return;
        }

        callbackFuture.completeExceptionally(new HabrCareerOAuthCallbackException(
                "Habr Career OAuth custom URI callback timed out"
        ));
    }

    private boolean completeExceptionally(String state, String message) {
        if (state == null || state.isBlank()) {
            log.warn("Habr Career OAuth custom URI callback failed without state");
            return false;
        }

        CompletableFuture<HabrCareerOAuthCallbackResult> callbackFuture =
                pendingCallbacks.remove(state);

        if (callbackFuture == null) {
            log.warn("Habr Career OAuth custom URI error callback received, but pending state was not found");
            return false;
        }

        callbackFuture.completeExceptionally(new HabrCareerOAuthCallbackException(message));

        return true;
    }

    private ParsedCustomSchemeCallback parseCallbackUri(String rawCallbackUri) {
        if (rawCallbackUri == null || rawCallbackUri.isBlank()) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback must not be blank"
            );
        }

        URI callbackUri;

        try {
            callbackUri = URI.create(rawCallbackUri);
        } catch (IllegalArgumentException exception) {
            throw new HabrCareerOAuthCallbackException(
                    "Invalid Habr Career OAuth custom URI callback",
                    exception
            );
        }

        validateCallbackUri(callbackUri);

        Map<String, String> queryParameters = parseQuery(callbackUri.getRawQuery());

        String state = queryParameters.get("state");
        String code = queryParameters.get("code");
        String error = queryParameters.get("error");

        if (state == null || state.isBlank()) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback does not contain state"
            );
        }

        if (error != null && !error.isBlank()) {
            return new ParsedCustomSchemeCallback(null, state, error);
        }

        if (code == null || code.isBlank()) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback does not contain authorization code"
            );
        }

        return new ParsedCustomSchemeCallback(code, state, null);
    }

    private void validateCallbackUri(URI callbackUri) {
        if (!SCHEME.equalsIgnoreCase(callbackUri.getScheme())) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback has unsupported scheme"
            );
        }

        if (!HOST.equalsIgnoreCase(callbackUri.getHost())) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback has unsupported host"
            );
        }

        if (!PATH.equals(callbackUri.getPath())) {
            throw new HabrCareerOAuthCallbackException(
                    "Habr Career OAuth custom URI callback has unsupported path"
            );
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        return Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(
                        java.util.stream.Collectors.toMap(
                                parts -> decode(parts[0]),
                                parts -> decode(parts[1]),
                                (first, second) -> first
                        )
                );
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @PreDestroy
    public void shutdown() {
        timeoutScheduler.shutdownNow();
    }

    private record ParsedCustomSchemeCallback(
            String code,
            String state,
            String error
    ) {
    }
}