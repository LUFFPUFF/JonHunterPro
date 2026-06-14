package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.ui.navigation.UiNavigator;
import ru.jobhunter.ui.session.CurrentUserSession;

/**
 * Controller for the main application window.
 */
@Component
public final class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final UiNavigator uiNavigator;
    private final CurrentUserSession currentUserSession;

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> navigationListView;

    public MainController(
            UiNavigator uiNavigator,
            CurrentUserSession currentUserSession
    ) {
        this.uiNavigator = uiNavigator;
        this.currentUserSession = currentUserSession;
    }

    /**
     * Initializes main window state after FXML loading.
     */
    @FXML
    public void initialize() {
        uiNavigator.attach(rootPane, statusLabel);

        navigationListView.getItems().setAll(
                "Профиль",
                "Автоотклики",
                "HR-контакты",
                "Холодные письма",
                "Оценка резюме",
                "Генерация резюме",
                "Настройки"
        );

        navigationListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> handleNavigation(newValue));

        if (currentUserSession.isAuthenticated()) {
            uiNavigator.showProfile();
            navigationListView.getSelectionModel().select("Профиль");
        } else {
            uiNavigator.showAuth();
        }

        log.info("Main window controller initialized");
    }

    /**
     * Handles application exit action.
     */
    @FXML
    public void onExit() {
        log.info("Exit action requested from main window");
        Platform.exit();
    }

    private void handleNavigation(String selectedItem) {
        if (selectedItem == null) {
            return;
        }

        if (!currentUserSession.isAuthenticated()) {
            uiNavigator.showAuth();
            return;
        }

        switch (selectedItem) {
            case "Профиль" -> uiNavigator.showProfile();
            case "Автоотклики" -> uiNavigator.showAutoResponses();
            case "HR-контакты" -> uiNavigator.showPlaceholder(
                    "HR-контакты",
                    "Раздел будет реализован на этапе сбора контактов HR."
            );
            case "Холодные письма" -> uiNavigator.showPlaceholder(
                    "Холодные письма",
                    "Раздел будет реализован на этапе рассылки холодных писем."
            );
            case "Оценка резюме" -> uiNavigator.showPlaceholder(
                    "Оценка резюме",
                    "Раздел будет реализован на этапе анализа резюме."
            );
            case "Генерация резюме" -> uiNavigator.showPlaceholder(
                    "Генерация резюме",
                    "Раздел будет реализован на этапе генерации резюме."
            );
            case "Настройки" -> uiNavigator.showPlaceholder(
                    "Настройки",
                    "Раздел настроек будет расширяться по мере добавления интеграций."
            );
            default -> uiNavigator.showProfile();
        }
    }
}