package ru.jobhunter.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.platform.hh.api.HhApiProperties;
import ru.jobhunter.infrastructure.platform.hh.api.HhApplicationTokenProperties;
import ru.jobhunter.infrastructure.platform.hh.auth.HhOAuthProperties;
import ru.jobhunter.infrastructure.platform.hh.autoresponse.HhAutoResponseProperties;
import ru.jobhunter.infrastructure.platform.hh.autoresponse.HhBrowserAutoResponseProperties;

@Configuration
@EnableConfigurationProperties({
        HhOAuthProperties.class,
        HhApiProperties.class,
        HhAutoResponseProperties.class,
        HhApplicationTokenProperties.class,
        HhBrowserAutoResponseProperties.class
})
public class HhIntegrationConfig {
}
