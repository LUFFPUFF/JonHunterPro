package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobhunter.integrations.hh.auto-response")
public record HhAutoResponseProperties(
        String defaultResumeId,
        String defaultMessage
) {

    public HhAutoResponseProperties {
        defaultResumeId = normalize(defaultResumeId);
        defaultMessage = normalize(defaultMessage);
    }

    public boolean hasDefaultResumeId() {
        return defaultResumeId != null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}