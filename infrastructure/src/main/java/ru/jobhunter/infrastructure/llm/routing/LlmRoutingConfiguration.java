package ru.jobhunter.infrastructure.llm.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.jobhunter.core.application.port.out.llm.LlmPort;

import java.util.Optional;

@Configuration
public class LlmRoutingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(
            LlmRoutingConfiguration.class
    );

    @Bean
    @ConditionalOnMissingBean(LlmPort.class)
    public LlmPort llmPort(
            @Qualifier("groqLlmProvider")
            ObjectProvider<LlmProvider> groqProvider
    ) {
        LlmProvider groq = groqProvider.getIfAvailable();

        if (groq == null) {
            log.warn(
                    "No active LLM provider is configured. "
                            + "Groq provider is disabled or unavailable."
            );

            return new UnavailableLlmPort();
        }

        log.info(
                "LLM routing configured: primaryProvider={}, "
                        + "automaticFallbackProvider=disabled",
                groq.providerId()
        );

        return new LlmRoutingPort(
                groq,
                Optional.empty()
        );
    }
}