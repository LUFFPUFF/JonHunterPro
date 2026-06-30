package ru.jobhunter.infrastructure.platform.habr.autoresponse;

public enum HabrCareerBrowserAutoResponseOutcome {

    RESPONSE_SENT_WITH_COVER_LETTER,
    RESPONSE_SENT_WITHOUT_COVER_LETTER,
    ALREADY_RESPONDED,
    PREFLIGHT_VERIFIED,
    CANDIDATE_APPROVAL_REQUIRED
}
