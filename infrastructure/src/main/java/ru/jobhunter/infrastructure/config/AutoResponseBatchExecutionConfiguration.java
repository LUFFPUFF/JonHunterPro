package ru.jobhunter.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AutoResponseBatchExecutionConfiguration {

    @Bean(destroyMethod = "close")
    @Qualifier("autoResponseBatchExecutor")
    public ExecutorService autoResponseBatchExecutor() {
        return Executors.newSingleThreadExecutor(
                Thread.ofVirtual()
                        .name(
                                "jobhunter-auto-response-batch-",
                                0
                        )
                        .factory()
        );
    }
}