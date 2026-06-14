package ru.jobhunter.core.application.dto;

public record HhVacancyDto(
        String externalId,
        String name,
        String url,
        String alternateUrl,
        String areaId,
        String areaName,
        String employerId,
        String employerName,
        String employerUrl,
        HhSalaryDto salary,
        String experienceId,
        String experienceName,
        String employmentId,
        String employmentName,
        String scheduleId,
        String scheduleName
) {
}
