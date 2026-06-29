package ru.jobhunter.infrastructure.platform.hh.autoresponse;

public enum HhQuestionnaireExecutionMode {

    DIAGNOSTIC_ONLY,
    APPLY;

    public boolean isDiagnosticOnly() {
        return this == DIAGNOSTIC_ONLY;
    }
}