package ru.jobhunter.infrastructure.platform.habr.autoresponse;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "jobhunter.integrations.habr-career.browser-auto-response"
)
public record HabrCareerBrowserAutoResponseProperties(
        boolean enabled,
        HabrCareerBrowserAutoResponseMode mode
) {

    public HabrCareerBrowserAutoResponseProperties {
        if (mode == null) {
            mode = HabrCareerBrowserAutoResponseMode.PREFLIGHT;
        }
    }
}
