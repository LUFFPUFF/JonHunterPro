package ru.jobhunter.core.application.dto;

import java.time.Instant;
import java.util.List;

public record HabrCareerPostResponseFormProbeResultDto(
        Status status,
        String requestedExternalVacancyId,
        String finalUrl,
        String pageTitle,
        String terminalResponseMarker,
        boolean responseExistsEditable,
        String responseEditActionSelector,
        boolean postResponseFormAvailable,
        String postResponseFormSelector,
        String coverLetterSelector,
        String complementSubmitSelector,
        List<HabrCareerResponseFormControlDto> controls,
        List<String> visibleButtonLabels,
        String diagnosticDirectory,
        Instant capturedAt
) {

    public HabrCareerPostResponseFormProbeResultDto {
        status = requireStatus(status);
        requestedExternalVacancyId = normalize(requestedExternalVacancyId);
        finalUrl = normalize(finalUrl);
        pageTitle = normalize(pageTitle);
        terminalResponseMarker = normalize(terminalResponseMarker);
        responseEditActionSelector = normalize(responseEditActionSelector);
        postResponseFormSelector = normalize(postResponseFormSelector);
        coverLetterSelector = normalize(coverLetterSelector);
        complementSubmitSelector = normalize(complementSubmitSelector);
        controls = controls == null ? List.of() : List.copyOf(controls);
        visibleButtonLabels = visibleButtonLabels == null
                ? List.of()
                : List.copyOf(visibleButtonLabels);
        diagnosticDirectory = normalize(diagnosticDirectory);
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
    }

    public enum Status {
        POST_RESPONSE_FORM_READY,
        RESPONSE_EXISTS_EDITABLE,
        TERMINAL_RESPONSE_STATE_WITHOUT_COMPLEMENT_FORM,
        INITIAL_RESPONSE_ACTION_STILL_AVAILABLE,
        NO_RESPONSE_STATE,
        AUTHENTICATION_REQUIRED,
        VACANCY_DETAILS_NOT_FOUND,
        UNEXPECTED_PAGE
    }

    private static Status requireStatus(Status value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Habr Career post-response form probe status must not be null"
            );
        }

        return value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
