package ru.jobhunter;

import javafx.application.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import ru.jobhunter.infrastructure.platform.hh.auth.HhOAuthCustomSchemeForwardClient;
import ru.jobhunter.ui.JobHunterApp;

public class Main {

    private Main() {}

    public static void main(String[] args) {
        if (HhOAuthCustomSchemeForwardClient.forwardToRunningInstanceIfNeeded(args)) {
            return;
        }

        JobHunterApp.setApplicationContextFactory(applicationArgs ->
                new SpringApplicationBuilder(JobHunterSpringApplication.class)
                        .web(WebApplicationType.NONE)
                        .headless(false)
                        .run(applicationArgs)
        );

        Application.launch(JobHunterApp.class, args);
    }
}
