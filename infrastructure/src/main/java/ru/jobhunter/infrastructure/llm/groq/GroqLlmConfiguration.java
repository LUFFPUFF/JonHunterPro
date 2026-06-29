package ru.jobhunter.infrastructure.llm.groq;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.llm.routing.LlmProvider;

@Configuration
@EnableConfigurationProperties(GroqProperties.class)
public class GroqLlmConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.groq",
            name = "enabled",
            havingValue = "true"
    )
    public OkHttpClient groqOkHttpClient(
            GroqProperties properties
    ) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.resolvedConnectTimeout())
                .readTimeout(properties.resolvedReadTimeout())
                .writeTimeout(properties.resolvedWriteTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.groq",
            name = "enabled",
            havingValue = "true"
    )
    public GroqTokenPerMinuteGovernor groqTokenPerMinuteGovernor() {
        return new GroqTokenPerMinuteGovernor();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.groq",
            name = "enabled",
            havingValue = "true"
    )
    public GroqLlmClient groqLlmClient(
            ObjectMapper objectMapper,
            @Qualifier("groqOkHttpClient") OkHttpClient httpClient,
            GroqProperties properties
    ) {
        return new GroqLlmClient(
                properties,
                objectMapper,
                httpClient
        );
    }

    @Bean(name = "groqLlmProvider")
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.groq",
            name = "enabled",
            havingValue = "true"
    )
    public LlmProvider groqLlmProvider(
            GroqProperties properties,
            GroqLlmClient groqLlmClient,
            GroqTokenPerMinuteGovernor groqTokenPerMinuteGovernor
    ) {
        properties.validateForEnabledProvider();

        return new GroqLlmAdapter(
                properties,
                groqLlmClient,
                groqTokenPerMinuteGovernor
        );
    }
}