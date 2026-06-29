package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HhQuestionnaireExecutionPropertiesTest {

    @Test
    void shouldUseDiagnosticOnlyModeByDefault() {
        HhQuestionnaireExecutionProperties properties =
                new HhQuestionnaireExecutionProperties();

        assertEquals(
                HhQuestionnaireExecutionMode.DIAGNOSTIC_ONLY,
                properties.executionMode()
        );

        assertTrue(properties.isDiagnosticOnly());
    }

    @Test
    void shouldAllowApplyModeToBeConfiguredExplicitly() {
        HhQuestionnaireExecutionProperties properties =
                new HhQuestionnaireExecutionProperties();

        properties.setExecutionMode(
                HhQuestionnaireExecutionMode.APPLY
        );

        assertEquals(
                HhQuestionnaireExecutionMode.APPLY,
                properties.executionMode()
        );

        assertFalse(properties.isDiagnosticOnly());
    }
}