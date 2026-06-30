package ru.jobhunter.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.platform.habr.api.HabrCareerApiProperties;
import ru.jobhunter.infrastructure.platform.habr.browser.HabrCareerBrowserSessionProperties;
import ru.jobhunter.infrastructure.platform.habr.autoresponse.HabrCareerBrowserAutoResponseProperties;
import ru.jobhunter.infrastructure.platform.habr.auth.HabrCareerOAuthProperties;

@Configuration
@EnableConfigurationProperties({
        HabrCareerApiProperties.class,
        HabrCareerOAuthProperties.class,
        HabrCareerBrowserSessionProperties.class,
        HabrCareerBrowserAutoResponseProperties.class
})
public class HabrCareerIntegrationConfig {
}
