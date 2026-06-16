package ru.jobhunter.infrastructure.platform.habr.auth;

import org.springframework.stereotype.Component;
import ru.jobhunter.infrastructure.platform.oauth.OAuthCustomSchemeCallbackHandler;

import java.util.Objects;

@Component
public final class HabrCareerOAuthCustomSchemeCallbackDispatcher implements OAuthCustomSchemeCallbackHandler {

    private static final String HABR_CAREER_CUSTOM_SCHEME_CALLBACK_PREFIX =
            "jobhunterpro://oauth/habr/callback";

    private final HabrCareerOAuthCustomSchemeCallbackRegistry callbackRegistry;

    public HabrCareerOAuthCustomSchemeCallbackDispatcher(
            HabrCareerOAuthCustomSchemeCallbackRegistry callbackRegistry
    ) {
        this.callbackRegistry = Objects.requireNonNull(
                callbackRegistry,
                "Habr Career OAuth custom scheme callback registry must not be null"
        );
    }

    @Override
    public boolean supports(String callbackUri) {
        return callbackUri != null
                && callbackUri.regionMatches(
                true,
                0,
                HABR_CAREER_CUSTOM_SCHEME_CALLBACK_PREFIX,
                0,
                HABR_CAREER_CUSTOM_SCHEME_CALLBACK_PREFIX.length()
        );
    }

    @Override
    public boolean dispatch(String callbackUri) {
        return callbackRegistry.completeFromCallbackUri(callbackUri);
    }
}