package ru.jobhunter.core.application.dto;

public record AddVacancyToAutoResponseQueueFailureDto(
        String externalVacancyId,
        String vacancyName,
        String message
) {

    public AddVacancyToAutoResponseQueueFailureDto {
        externalVacancyId = normalize(
                externalVacancyId,
                "unknown"
        );

        vacancyName = normalize(
                vacancyName,
                "Без названия"
        );

        message = normalize(
                message,
                "Неизвестная ошибка"
        );
    }

    private static String normalize(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}