package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
public final class HhOAuthCustomSchemeCallbackDispatcher {

    private static final String CUSTOM_SCHEME_PREFIX = "jobhunterpro://";

    private final HhOAuthCustomSchemeCallbackRegistry callbackRegistry;

    public HhOAuthCustomSchemeCallbackDispatcher(
            HhOAuthCustomSchemeCallbackRegistry callbackRegistry
    ) {
        this.callbackRegistry = Objects.requireNonNull(
                callbackRegistry,
                "HH OAuth custom scheme callback registry must not be null"
        );
    }

    public boolean dispatchIfSupported(String argument) {
        if (!isCustomSchemeCallback(argument)) {
            return false;
        }

        return callbackRegistry.completeFromCallbackUri(argument);
    }

    public int dispatchAll(Collection<String> arguments) {
        Objects.requireNonNull(arguments, "Startup arguments must not be null");

        int dispatchedCount = 0;

        for (String argument : arguments) {
            if (dispatchIfSupported(argument)) {
                dispatchedCount++;
            }
        }

        return dispatchedCount;
    }

    private boolean isCustomSchemeCallback(String value) {
        return value != null
                && value.regionMatches(
                true,
                0,
                CUSTOM_SCHEME_PREFIX,
                0,
                CUSTOM_SCHEME_PREFIX.length()
        );
    }
}