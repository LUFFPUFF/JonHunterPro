package ru.jobhunter.ui.loader;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class FxmlViewLoader {

    private final ApplicationContext applicationContext;

    public FxmlViewLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Parent load(String resourcePath) {
        String normalizedPath = normalizePath(resourcePath);
        URL resource = Thread.currentThread()
                .getContextClassLoader()
                .getResource(normalizedPath);

        if (resource == null) {
            resource = FxmlViewLoader.class.getResource("/" + normalizedPath);
        }

        if (resource == null) {
            throw new IllegalStateException("FXML resource not found: /" + normalizedPath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(applicationContext::getBean);

        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load FXML resource: /" + normalizedPath, exception);
        }
    }

    private String normalizePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("FXML resource path must not be blank");
        }

        return resourcePath.startsWith("/")
                ? resourcePath.substring(1)
                : resourcePath;
    }
}
