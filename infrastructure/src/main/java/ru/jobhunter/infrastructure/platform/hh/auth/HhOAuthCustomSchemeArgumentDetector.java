package ru.jobhunter.infrastructure.platform.hh.auth;

import java.util.Arrays;
import java.util.List;

public final class HhOAuthCustomSchemeArgumentDetector {

    private static final String CUSTOM_SCHEME_PREFIX = "jobhunterpro://";

    private HhOAuthCustomSchemeArgumentDetector() {
    }

    public static boolean containsCustomSchemeCallback(String[] args) {
        return !findCustomSchemeCallbacks(args).isEmpty();
    }

    public static List<String> findCustomSchemeCallbacks(String[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }

        return Arrays.stream(args)
                .filter(HhOAuthCustomSchemeArgumentDetector::isCustomSchemeCallback)
                .toList();
    }

    private static boolean isCustomSchemeCallback(String value) {
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