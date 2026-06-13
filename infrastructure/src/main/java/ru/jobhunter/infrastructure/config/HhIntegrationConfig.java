package ru.jobhunter.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.platform.hh.auth.HhOAuthProperties;

@Configuration
@EnableConfigurationProperties(HhOAuthProperties.class)
public class HhIntegrationConfig {
}
