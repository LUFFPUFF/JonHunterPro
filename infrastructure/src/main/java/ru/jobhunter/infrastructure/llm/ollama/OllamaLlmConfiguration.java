package ru.jobhunter.infrastructure.llm.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.infrastructure.llm.routing.LlmProvider;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaLlmConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.ollama",
            name = "enabled",
            havingValue = "true"
    )
    public OkHttpClient ollamaOkHttpClient(OllamaProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.resolvedConnectTimeout())
                .readTimeout(properties.resolvedReadTimeout())
                .writeTimeout(properties.resolvedWriteTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.ollama",
            name = "enabled",
            havingValue = "true"
    )
    public OllamaLlmClient ollamaLlmClient(
            ObjectMapper objectMapper,
            @Qualifier("ollamaOkHttpClient") OkHttpClient okHttpClient,
            OllamaProperties properties
    ) {
        return new OllamaLlmClient(
                properties,
                objectMapper,
                okHttpClient
        );
    }

    @Bean(name = "ollamaLlmProvider")
    @ConditionalOnProperty(
            prefix = "jobhunter.llm.ollama",
            name = "enabled",
            havingValue = "true"
    )
    public LlmProvider ollamaLlmProvider(
            OllamaProperties properties,
            OllamaLlmClient ollamaLlmClient
    ) {
        properties.validateForEnabledProvider();

        return new OllamaLlmAdapter(
                properties,
                ollamaLlmClient
        );
    }
}
