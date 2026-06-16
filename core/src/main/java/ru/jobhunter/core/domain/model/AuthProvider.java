package ru.jobhunter.core.domain.model;

import java.util.Arrays;

public enum AuthProvider {

    HH_RU("HH_RU"),
    HABR_CAREER("HABR_CAREER");

    private final String code;

    AuthProvider(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AuthProvider fromCode(String code) {
        return Arrays.stream(values())
                .filter(provider -> provider.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported auth provider: " + code));
    }
}
