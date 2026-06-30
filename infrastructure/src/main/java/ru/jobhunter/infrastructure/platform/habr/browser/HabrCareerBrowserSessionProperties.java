package ru.jobhunter.infrastructure.platform.habr.browser;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "jobhunter.integrations.habr-career.browser-session")
public record HabrCareerBrowserSessionProperties(
        boolean headless,
        String userDataDir,
        int waitTimeoutSeconds,
        int interactiveLoginTimeoutSeconds
) {

    private static final int DEFAULT_WAIT_TIMEOUT_SECONDS = 20;
    private static final int DEFAULT_INTERACTIVE_LOGIN_TIMEOUT_SECONDS = 300;

    public HabrCareerBrowserSessionProperties {
        userDataDir = normalize(userDataDir);

        if (userDataDir == null) {
            userDataDir = Path.of(
                    System.getProperty("user.home"),
                    ".jobhunterpro",
                    "habr-career-chrome-profile"
            ).toString();
        }

        if (waitTimeoutSeconds <= 0) {
            waitTimeoutSeconds = DEFAULT_WAIT_TIMEOUT_SECONDS;
        }

        if (interactiveLoginTimeoutSeconds <= 0) {
            interactiveLoginTimeoutSeconds = DEFAULT_INTERACTIVE_LOGIN_TIMEOUT_SECONDS;
        }
    }

    public Duration waitTimeout() {
        return Duration.ofSeconds(waitTimeoutSeconds);
    }

    public Duration interactiveLoginTimeout() {
        return Duration.ofSeconds(interactiveLoginTimeoutSeconds);
    }

    public String requireUserDataDir() {
        return userDataDir;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
