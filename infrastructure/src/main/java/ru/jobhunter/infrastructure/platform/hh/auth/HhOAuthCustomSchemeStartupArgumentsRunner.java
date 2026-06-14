package ru.jobhunter.infrastructure.platform.hh.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Component
public final class HhOAuthCustomSchemeStartupArgumentsRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HhOAuthCustomSchemeStartupArgumentsRunner.class);

    private final HhOAuthCustomSchemeCallbackDispatcher callbackDispatcher;

    public HhOAuthCustomSchemeStartupArgumentsRunner(
            HhOAuthCustomSchemeCallbackDispatcher callbackDispatcher
    ) {
        this.callbackDispatcher = Objects.requireNonNull(
                callbackDispatcher,
                "HH OAuth custom scheme callback dispatcher must not be null"
        );
    }

    @Override
    public void run(ApplicationArguments args) {
        Objects.requireNonNull(args, "Application arguments must not be null");

        int dispatchedCount = callbackDispatcher.dispatchAll(
                Arrays.asList(args.getSourceArgs())
        );

        if (dispatchedCount > 0) {
            log.info("HH OAuth custom URI startup callbacks dispatched: count={}", dispatchedCount);
        }
    }
}