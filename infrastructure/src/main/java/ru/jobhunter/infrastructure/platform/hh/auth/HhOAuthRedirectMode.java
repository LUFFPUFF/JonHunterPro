package ru.jobhunter.infrastructure.platform.hh.auth;

public enum HhOAuthRedirectMode {

    LOCAL_HTTP_SERVER,
    CUSTOM_URI_SCHEME;

    public static HhOAuthRedirectMode from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL_HTTP_SERVER;
        }

        String normalizedValue = value.trim()
                .replace("-", "_")
                .toUpperCase();

        for (HhOAuthRedirectMode mode : values()) {
            if (mode.name().equals(normalizedValue)) {
                return mode;
            }
        }

        throw new HhOAuthConfigurationException("Unsupported HH OAuth redirect mode: " + value);
    }
}
