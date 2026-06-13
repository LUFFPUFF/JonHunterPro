package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AuthenticatedUserDto;
import ru.jobhunter.core.application.usecase.integration.ConnectHhAccountUseCase;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.ui.navigation.UiNavigator;
import ru.jobhunter.ui.session.CurrentUserSession;

import java.awt.Desktop;
import java.net.URI;
import java.util.Objects;

@Component
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final CurrentUserSession currentUserSession;
    private final UiNavigator uiNavigator;
    private final ConnectHhAccountUseCase connectHhAccountUseCase;

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label userIdLabel;

    @FXML
    private Button connectHhButton;

    @FXML
    private Label hhConnectionStatusLabel;

    public ProfileController(
            CurrentUserSession currentUserSession,
            UiNavigator uiNavigator,
            ConnectHhAccountUseCase connectHhAccountUseCase
    ) {
        this.currentUserSession = Objects.requireNonNull(
                currentUserSession,
                "Current user session must not be null"
        );
        this.uiNavigator = Objects.requireNonNull(
                uiNavigator,
                "UI navigator must not be null"
        );
        this.connectHhAccountUseCase = Objects.requireNonNull(
                connectHhAccountUseCase,
                "Connect HH account use case must not be null"
        );
    }

    @FXML
    public void initialize() {
        AuthenticatedUserDto user = currentUserSession.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Profile screen requires authenticated user"));

        fullNameLabel.setText(user.fullName());
        emailLabel.setText(user.email());
        userIdLabel.setText(user.id().toString());

        setHhStatus("HH.ru не подключён");

        log.info("Profile screen initialized: userId={}, email={}", user.id(), user.email());
    }

    @FXML
    public void onLogout() {
        currentUserSession.getCurrentUser()
                .ifPresent(user -> log.info("User logged out: userId={}, email={}", user.id(), user.email()));

        currentUserSession.clear();
        uiNavigator.showAuth();
    }

    @FXML
    private void onConnectHhClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setHhStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        connectHhButton.setDisable(true);
        setHhStatus("Открываем страницу авторизации HH.ru...");

        try {
            UserId userId = UserId.of(currentUser.id());

            var flow = connectHhAccountUseCase.startConnection(userId);

            openBrowser(flow.authorizationUrl());

            setHhStatus("Авторизуйтесь в браузере. После разрешения доступа вернитесь в приложение.");

            flow.completion().whenComplete((result, throwable) -> Platform.runLater(() -> {
                connectHhButton.setDisable(false);

                if (throwable == null) {
                    setHhStatus("HH.ru подключён.");
                } else {
                    setHhStatus("Не удалось подключить HH.ru: " + rootMessage(throwable));
                }
            }));
        } catch (RuntimeException exception) {
            connectHhButton.setDisable(false);
            setHhStatus("Не удалось начать подключение HH.ru: " + rootMessage(exception));
        }
    }

    private void openBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Открытие браузера не поддерживается на этой системе");
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException("Действие открытия браузера не поддерживается");
            }

            desktop.browse(URI.create(url));
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось открыть браузер", exception);
        }
    }

    private void setHhStatus(String message) {
        if (hhConnectionStatusLabel != null) {
            hhConnectionStatusLabel.setText(message);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}