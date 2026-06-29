package ru.jobhunter.infrastructure.platform.hh.autoresponse;

public enum HhBrowserAutoResponseMode {
    PREFLIGHT, EXECUTE;

    public boolean isPreflight() {
        return this == PREFLIGHT;
    }
}
