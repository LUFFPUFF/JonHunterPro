package ru.jobhunter.infrastructure.platform.habr.autoresponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HabrCareerBrowserAutoResponsePropertiesTest {

    @Test
    void defaultsToPreflightModeWhenModeIsMissing() {
        HabrCareerBrowserAutoResponseProperties properties =
                new HabrCareerBrowserAutoResponseProperties(false, null);

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.mode())
                .isEqualTo(HabrCareerBrowserAutoResponseMode.PREFLIGHT);
    }
}
