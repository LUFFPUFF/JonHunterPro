package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "jobhunter.hh.questionnaire")
public class HhQuestionnaireExecutionProperties {

    private HhQuestionnaireExecutionMode executionMode =
            HhQuestionnaireExecutionMode.DIAGNOSTIC_ONLY;

    public HhQuestionnaireExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(
            HhQuestionnaireExecutionMode executionMode
    ) {
        this.executionMode = Objects.requireNonNull(
                executionMode,
                "HH questionnaire execution mode must not be null"
        );
    }

    public HhQuestionnaireExecutionMode executionMode() {
        return executionMode;
    }

    public boolean isDiagnosticOnly() {
        return executionMode.isDiagnosticOnly();
    }
}