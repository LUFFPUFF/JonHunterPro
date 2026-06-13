package ru.jobhunter.ui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jobhunter.ui.loader.FxmlViewLoader;

import javax.naming.Context;
import java.util.Objects;
import java.util.function.Function;

public class JobHunterApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JobHunterApp.class);

    private static Function<String[], ConfigurableApplicationContext> applicationContextFactory;

    private ConfigurableApplicationContext applicationContext;

    public static void setApplicationContextFactory(
            Function<String[], ConfigurableApplicationContext> factory
    ) {
        applicationContextFactory = Objects.requireNonNull(factory, "Application context factory must not be null");
    }

    @Override
    public void init() {
        if (applicationContextFactory == null) {
            throw new IllegalStateException("Application context factory is not configured");
        }

        String[] args = getParameters().getRaw().toArray(String[]::new);
        applicationContext = applicationContextFactory.apply(args);
        log.info("Spring application context initialized");
    }

    @Override
    public void start(Stage primaryStage) {
        FxmlViewLoader fxmlViewLoader = applicationContext.getBean(FxmlViewLoader.class);

        Parent root = fxmlViewLoader.load("/ru/jobhunter/ui/view/main.fxml");

        Scene scene = new Scene(root, 1100, 720);

        primaryStage.setTitle("JobHunterPro");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);
        primaryStage.show();

        log.info("JavaFX primary stage displayed");
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
            log.info("Spring application context closed");
        }
    }
}
