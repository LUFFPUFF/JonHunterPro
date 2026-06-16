package ru.jobhunter.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.platform.habr.api.HabrCareerApiProperties;
import ru.jobhunter.infrastructure.platform.habr.auth.HabrCareerOAuthProperties;

@Configuration
@EnableConfigurationProperties({
        HabrCareerApiProperties.class,
        HabrCareerOAuthProperties.class
})
public class HabrCareerIntegrationConfig {
}
