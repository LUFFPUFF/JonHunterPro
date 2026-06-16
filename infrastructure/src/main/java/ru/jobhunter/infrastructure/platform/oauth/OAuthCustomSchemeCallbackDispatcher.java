package ru.jobhunter.infrastructure.platform.oauth;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class OAuthCustomSchemeCallbackDispatcher {

    private final List<OAuthCustomSchemeCallbackHandler> handlers;

    public OAuthCustomSchemeCallbackDispatcher(List<OAuthCustomSchemeCallbackHandler> handlers) {
        Objects.requireNonNull(handlers, "OAuth custom scheme callback handlers must not be null");
        this.handlers = List.copyOf(handlers);
    }

    public boolean dispatchIfSupported(String argument) {
        if (argument == null || argument.isBlank()) {
            return false;
        }

        for (OAuthCustomSchemeCallbackHandler handler : handlers) {
            if (handler.supports(argument)) {
                return handler.dispatch(argument);
            }
        }

        return false;
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
}
