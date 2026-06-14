package ru.jobhunter.infrastructure.platform.hh.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record HhVacancySearchRequest(
        String text,
        String area,
        Integer page,
        Integer perPage
) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PER_PAGE = 20;
    private static final int MIN_PAGE = 0;
    private static final int MIN_PER_PAGE = 1;
    private static final int MAX_PER_PAGE = 100;

    public HhVacancySearchRequest {
        page = page == null ? DEFAULT_PAGE : page;
        perPage = perPage == null ? DEFAULT_PER_PAGE : perPage;

        if (page < MIN_PAGE) {
            throw new IllegalArgumentException("HH vacancy search page must not be negative");
        }

        if (perPage < MIN_PER_PAGE || perPage > MAX_PER_PAGE) {
            throw new IllegalArgumentException("HH vacancy search perPage must be between 1 and 100");
        }

        text = normalize(text);
        area = normalize(area);
    }

    public static HhVacancySearchRequest ofText(String text) {
        return new HhVacancySearchRequest(text, null, DEFAULT_PAGE, DEFAULT_PER_PAGE);
    }

    public Map<String, String> toQueryParameters() {
        Map<String, String> parameters = new LinkedHashMap<>();

        putIfNotBlank(parameters, "text", text);
        putIfNotBlank(parameters, "area", area);

        parameters.put("page", String.valueOf(page));
        parameters.put("per_page", String.valueOf(perPage));

        return Map.copyOf(parameters);
    }

    private static void putIfNotBlank(
            Map<String, String> parameters,
            String name,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            parameters.put(name, value);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
