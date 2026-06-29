package ru.jobhunter.infrastructure.llm.openrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.llm.routing.LlmProvider;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderCircuitBreaker;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(OpenRouterProperties.class)
public class OpenRouterLlmConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.openrouter",
            name = "enabled",
            havingValue = "true"
    )
    public OkHttpClient openRouterOkHttpClient(OpenRouterProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.resolvedConnectTimeout())
                .readTimeout(properties.resolvedReadTimeout())
                .writeTimeout(properties.resolvedWriteTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.openrouter",
            name = "enabled",
            havingValue = "true"
    )
    public OpenRouterLlmClient openRouterLlmClient(
            ObjectMapper objectMapper,
            @Qualifier("openRouterOkHttpClient") OkHttpClient okHttpClient,
            OpenRouterProperties properties
    ) {
        return new OpenRouterLlmClient(
                properties,
                objectMapper,
                okHttpClient
        );
    }

    @Bean(name = "openRouterLlmProvider")
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.openrouter",
            name = "enabled",
            havingValue = "true"
    )
    public LlmProvider openRouterLlmProvider(
            OpenRouterProperties properties,
            OpenRouterLlmClient openRouterLlmClient,
            LlmProviderCircuitBreaker circuitBreaker,
            @Qualifier("llmCircuitBreakerClock") Clock clock
    ) {
        properties.validateForEnabledProvider();

        return new OpenRouterLlmAdapter(
                properties,
                openRouterLlmClient,
                circuitBreaker,
                clock
        );
    }
}
