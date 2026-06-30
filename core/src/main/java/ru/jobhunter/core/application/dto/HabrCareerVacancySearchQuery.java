package ru.jobhunter.core.application.dto;

public record HabrCareerVacancySearchQuery(
        String query,
        int page
) {

    public HabrCareerVacancySearchQuery {
        query = normalize(query);

        if (page < 1) {
            throw new IllegalArgumentException(
                    "Habr Career search page must be greater than or equal to 1"
            );
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
