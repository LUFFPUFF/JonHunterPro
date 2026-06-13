package ru.jobhunter.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor", destroyMethod = "close")
    public ExecutorService applicationTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
