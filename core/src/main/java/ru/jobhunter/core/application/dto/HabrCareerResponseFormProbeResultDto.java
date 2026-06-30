package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.List;

public record HabrCareerResponseFormProbeResultDto(
        Status status,
        String requestedExternalVacancyId,
        String finalUrl,
        String pageTitle,
        boolean initialResponseActionClicked,
        String initialResponseActionSelector,
        String responseContainerSelector,
        List<HabrCareerResponseFormControlDto> controls,
        List<String> visibleButtonLabels,
        String diagnosticDirectory,
        Instant capturedAt
) {

    public HabrCareerResponseFormProbeResultDto {
        status = requireStatus(status);
        requestedExternalVacancyId = normalize(requestedExternalVacancyId);
        finalUrl = normalize(finalUrl);
        pageTitle = normalize(pageTitle);
        initialResponseActionSelector = normalize(initialResponseActionSelector);
        responseContainerSelector = normalize(responseContainerSelector);
        controls = controls == null ? List.of() : List.copyOf(controls);
        visibleButtonLabels = visibleButtonLabels == null
                ? List.of()
                : List.copyOf(visibleButtonLabels);
        diagnosticDirectory = normalize(diagnosticDirectory);
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }

    public enum Status {
        DIRECT_RESPONSE_WOULD_SEND_IMMEDIATELY,
        RESPONSE_ACTION_PRESENT_NOT_CLICKED_FOR_SAFETY,
        INITIAL_RESPONSE_ACTION_NOT_AVAILABLE,
        RESPONSE_FORM_READY,
        TERMINAL_RESPONSE_STATE_DETECTED,
        RESPONSE_FORM_NOT_FOUND,
        AUTHENTICATION_REQUIRED,
        VACANCY_DETAILS_NOT_FOUND,
        UNEXPECTED_PAGE
    }

    private static Status requireStatus(Status value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Habr Career response form probe status must not be null"
            );
        }

        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
