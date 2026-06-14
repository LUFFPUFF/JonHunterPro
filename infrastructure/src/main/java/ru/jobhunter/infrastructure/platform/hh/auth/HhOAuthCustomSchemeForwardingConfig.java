package ru.jobhunter.infrastructure.platform.hh.auth;

public final class HhOAuthCustomSchemeForwardingConfig {

    public static final String ENV_FORWARD_PORT = "HH_CUSTOM_SCHEME_FORWARD_PORT";
    public static final String SYSTEM_PROPERTY_FORWARD_PORT = "jobhunter.hh.custom-scheme-forward-port";
    public static final int DEFAULT_FORWARD_PORT = 54347;

    private HhOAuthCustomSchemeForwardingConfig() {
    }

    public static int resolveForwardPort() {
        String systemPropertyValue = System.getProperty(SYSTEM_PROPERTY_FORWARD_PORT);

        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return parsePort(systemPropertyValue);
        }

        String envValue = System.getenv(ENV_FORWARD_PORT);

        if (envValue != null && !envValue.isBlank()) {
            return parsePort(envValue);
        }

        return DEFAULT_FORWARD_PORT;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());

            if (port < 1024 || port > 65535) {
                throw new HhOAuthConfigurationException(
                        "HH custom scheme forward port must be between 1024 and 65535"
                );
            }

            return port;
        } catch (NumberFormatException exception) {
            throw new HhOAuthConfigurationException(
                    "HH custom scheme forward port must be a valid integer"
            );
        }
    }
}