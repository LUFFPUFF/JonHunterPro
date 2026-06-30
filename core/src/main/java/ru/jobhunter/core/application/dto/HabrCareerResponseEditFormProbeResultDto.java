package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.List;

public record HabrCareerResponseEditFormProbeResultDto(
        Status status,
        String requestedExternalVacancyId,
        String finalUrl,
        String pageTitle,
        boolean responseExistsEditable,
        boolean editActionClicked,
        boolean editFormReady,
        String responseContainerSelector,
        String editActionSelector,
        List<HabrCareerResponseFormControlDto> controls,
        List<String> visibleButtonLabels,
        String diagnosticDirectory,
        Instant capturedAt
) {

    public HabrCareerResponseEditFormProbeResultDto {
        status = requireStatus(status);
        requestedExternalVacancyId = normalize(requestedExternalVacancyId);
        finalUrl = normalize(finalUrl);
        pageTitle = normalize(pageTitle);
        responseContainerSelector = normalize(responseContainerSelector);
        editActionSelector = normalize(editActionSelector);
        controls = controls == null ? List.of() : List.copyOf(controls);
        visibleButtonLabels = visibleButtonLabels == null
                ? List.of()
                : List.copyOf(visibleButtonLabels);
        diagnosticDirectory = normalize(diagnosticDirectory);
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }

    public enum Status {
        EDIT_FORM_READY,
        RESPONSE_EXISTS_EDIT_FORM_NOT_RENDERED,
        RESPONSE_EXISTS_EDIT_ACTION_NOT_FOUND,
        RESPONSE_NOT_FOUND,
        AUTHENTICATION_REQUIRED,
        VACANCY_DETAILS_NOT_FOUND,
        UNEXPECTED_PAGE
    }

    private static Status requireStatus(Status value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Habr Career response edit form probe status must not be null"
            );
        }

        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
