package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jobhunter.integrations.hh.browser-auto-response")
public record HhBrowserAutoResponseProperties(boolean enabled, HhBrowserAutoResponseMode mode, boolean headless,
                                              String userDataDir, int waitTimeoutSeconds) {
    private static final int DEFAULT_WAIT_TIMEOUT_SECONDS = 30;

    public HhBrowserAutoResponseProperties {
        mode = mode == null ? HhBrowserAutoResponseMode.PREFLIGHT : mode;
        userDataDir = normalize(userDataDir);
        if (waitTimeoutSeconds <= 0) {
            waitTimeoutSeconds = DEFAULT_WAIT_TIMEOUT_SECONDS;
        }
    }

    public Duration waitTimeout() {
        return Duration.ofSeconds(waitTimeoutSeconds);
    }

    public String requireUserDataDir() {
        if (userDataDir == null) {
            throw new HhAutoResponseExecutionException("HH browser profile directory is not configured. " + "Set HH_BROWSER_USER_DATA_DIR.");
        }
        return userDataDir;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}