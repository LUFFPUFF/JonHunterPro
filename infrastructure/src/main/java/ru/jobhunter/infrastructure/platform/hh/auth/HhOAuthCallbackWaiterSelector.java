package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class HhOAuthCallbackWaiterSelector implements HhOAuthCallbackWaiter {

    private final HhOAuthProperties properties;
    private final Map<HhOAuthRedirectMode, HhOAuthCallbackStrategy> strategiesByMode;


    public HhOAuthCallbackWaiterSelector(
            HhOAuthProperties properties,
            List<HhOAuthCallbackStrategy> strategies
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "HH OAuth properties must not be null"
        );
        Objects.requireNonNull(strategies, "HH OAuth callback strategies must not be null");

        this.strategiesByMode = mapStrategies(strategies);
    }

    @Override
    public CompletableFuture<HhOAuthCallbackResult> waitForCallback(
            String expectedState,
            Duration timeout
    ) {
        Objects.requireNonNull(expectedState, "Expected OAuth state must not be null");
        Objects.requireNonNull(timeout, "OAuth callback timeout must not be null");

        HhOAuthRedirectMode redirectMode = properties.parsedRedirectMode();

        HhOAuthCallbackStrategy strategy = strategiesByMode.get(redirectMode);

        if (strategy == null) {
            throw new HhOAuthCallbackException(
                    "HH OAuth callback strategy is not configured for redirect mode: " + redirectMode
            );
        }

        return strategy.waitForCallback(expectedState, timeout);
    }

    private Map<HhOAuthRedirectMode, HhOAuthCallbackStrategy> mapStrategies(
            List<HhOAuthCallbackStrategy> strategies
    ) {
        Map<HhOAuthRedirectMode, HhOAuthCallbackStrategy> result =
                new EnumMap<>(HhOAuthRedirectMode.class);

        for (HhOAuthCallbackStrategy strategy : strategies) {
            HhOAuthRedirectMode redirectMode = strategy.redirectMode();

            HhOAuthCallbackStrategy previous = result.putIfAbsent(redirectMode, strategy);

            if (previous != null) {
                throw new HhOAuthConfigurationException(
                        "Duplicate HH OAuth callback strategy for redirect mode: " + redirectMode
                );
            }
        }

        return Map.copyOf(result);
    }
}
