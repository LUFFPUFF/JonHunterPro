package ru.jobhunter.infrastructure.platform.hh.autoresponse;

public enum HhBrowserAutoResponseOutcome {

    RESPONSE_SENT_WITH_COVER_LETTER,
    RESPONSE_SENT_WITHOUT_COVER_LETTER,
    ALREADY_RESPONDED,
    PREFLIGHT_VERIFIED,
    QUESTIONNAIRE_REQUIRED,
    QUESTIONNAIRE_FILLED_REVIEW_REQUIRED
}