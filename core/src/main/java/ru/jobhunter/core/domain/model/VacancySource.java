package ru.jobhunter.core.domain.model;

import java.util.Arrays;

public enum VacancySource {

    HH_RU("HH_RU"),
    HABR_CAREER("HABR_CAREER");

    private final String code;

    VacancySource(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static VacancySource fromCode(String code) {
        return Arrays.stream(values())
                .filter(source -> source.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported vacancy source: " + code));
    }

}
