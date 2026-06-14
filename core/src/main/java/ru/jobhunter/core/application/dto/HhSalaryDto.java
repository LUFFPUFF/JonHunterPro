package ru.jobhunter.core.application.dto;

public record HhSalaryDto(
        Integer from,
        Integer to,
        String currency,
        Boolean gross
) {
}
