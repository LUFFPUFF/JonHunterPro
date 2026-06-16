package ru.jobhunter.infrastructure.platform.hh.auth;

import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.oauth.OAuthCustomSchemeCallbackHandler;

import java.util.Collection;
import java.util.Objects;

@Component
public final class HhOAuthCustomSchemeCallbackDispatcher implements OAuthCustomSchemeCallbackHandler {

    private static final String HH_CUSTOM_SCHEME_CALLBACK_PREFIX =
            "jobhunterpro://oauth/hh/callback";

    private final HhOAuthCustomSchemeCallbackRegistry callbackRegistry;

    public HhOAuthCustomSchemeCallbackDispatcher(
            HhOAuthCustomSchemeCallbackRegistry callbackRegistry
    ) {
        this.callbackRegistry = Objects.requireNonNull(
                callbackRegistry,
                "HH OAuth custom scheme callback registry must not be null"
        );
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

    @Override
    public boolean supports(String callbackUri) {
        return callbackUri != null
                && callbackUri.regionMatches(
                true,
                0,
                HH_CUSTOM_SCHEME_CALLBACK_PREFIX,
                0,
                HH_CUSTOM_SCHEME_CALLBACK_PREFIX.length()
        );
    }

    @Override
    public boolean dispatch(String callbackUri) {
        return callbackRegistry.completeFromCallbackUri(callbackUri);
    }

    public boolean dispatchIfSupported(String argument) {
        if (!supports(argument)) {
            return false;
        }

        return dispatch(argument);
    }
}