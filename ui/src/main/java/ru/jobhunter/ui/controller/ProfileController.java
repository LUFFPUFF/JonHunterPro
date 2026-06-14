package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AuthenticatedUserDto;
import ru.jobhunter.core.application.dto.HhConnectionStatusDto;
import ru.jobhunter.core.application.dto.HhCurrentUserDto;
import ru.jobhunter.core.application.usecase.integration.ConnectHhAccountUseCase;
import ru.jobhunter.core.application.usecase.integration.GetHhConnectionStatusUseCase;
import ru.jobhunter.core.application.usecase.integration.GetHhCurrentUserUseCase;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.ui.navigation.UiNavigator;
import ru.jobhunter.ui.session.CurrentUserSession;

import java.awt.Desktop;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private static final DateTimeFormatter TOKEN_EXPIRATION_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final CurrentUserSession currentUserSession;
    private final UiNavigator uiNavigator;
    private final ConnectHhAccountUseCase connectHhAccountUseCase;
    private final GetHhConnectionStatusUseCase getHhConnectionStatusUseCase;
    private final GetHhCurrentUserUseCase getHhCurrentUserUseCase;

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

    @FXML
    private Button checkHhApiButton;

    @FXML
    private Label hhApiStatusLabel;

    public ProfileController(
            CurrentUserSession currentUserSession,
            UiNavigator uiNavigator,
            ConnectHhAccountUseCase connectHhAccountUseCase,
            GetHhConnectionStatusUseCase getHhConnectionStatusUseCase,
            GetHhCurrentUserUseCase getHhCurrentUserUseCase
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
        this.getHhConnectionStatusUseCase = Objects.requireNonNull(
                getHhConnectionStatusUseCase,
                "Get HH connection status use case must not be null"
        );
        this.getHhCurrentUserUseCase = Objects.requireNonNull(
                getHhCurrentUserUseCase,
                "Get HH current user use case must not be null"
        );
    }

    @FXML
    public void initialize() {
        AuthenticatedUserDto user = currentUserSession.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Profile screen requires authenticated user"));

        fullNameLabel.setText(user.fullName());
        emailLabel.setText(user.email());
        userIdLabel.setText(user.id().toString());

        setHhApiStatus("HH API не проверялся");

        UserId userId = UserId.of(user.id());
        loadHhConnectionStatus(userId);

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
                    loadHhConnectionStatus(userId);
                    checkHhApi(userId);
                } else {
                    setHhStatus("Не удалось подключить HH.ru: " + rootMessage(throwable));
                }
            }));
        } catch (RuntimeException exception) {
            connectHhButton.setDisable(false);
            setHhStatus("Не удалось начать подключение HH.ru: " + rootMessage(exception));
        }
    }

    @FXML
    private void onCheckHhApiClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setHhApiStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        UserId userId = UserId.of(currentUser.id());
        checkHhApi(userId);
    }

    private void checkHhApi(UserId userId) {
        checkHhApiButton.setDisable(true);
        setHhApiStatus("Проверяем доступ к HH API...");

        getHhCurrentUserUseCase.getCurrentUser(userId)
                .whenComplete((hhUser, throwable) -> Platform.runLater(() -> {
                    checkHhApiButton.setDisable(false);

                    if (throwable == null) {
                        setHhApiStatus(formatHhApiStatus(hhUser));
                    } else {
                        setHhApiStatus("HH API недоступен: " + rootMessage(throwable));
                    }
                }));
    }

    private void loadHhConnectionStatus(UserId userId) {
        setHhStatus("Проверяем статус HH.ru...");

        getHhConnectionStatusUseCase.getStatus(userId)
                .whenComplete((status, throwable) -> Platform.runLater(() -> {
                    if (throwable == null) {
                        setHhStatus(formatHhStatus(status));
                    } else {
                        setHhStatus("Не удалось проверить статус HH.ru: " + rootMessage(throwable));
                    }
                }));
    }

    private String formatHhStatus(HhConnectionStatusDto status) {
        return switch (status.status()) {
            case DISCONNECTED -> "HH.ru не подключён";
            case CONNECTED -> "HH.ru подключён. Токен действителен до "
                    + TOKEN_EXPIRATION_FORMATTER.format(status.expiresAt());
            case EXPIRED -> "HH.ru подключён, но токен истёк. Требуется обновление.";
        };
    }

    private String formatHhApiStatus(HhCurrentUserDto hhUser) {
        String userType = valueOrUnknown(hhUser.userType());
        String email = valueOrUnknown(hhUser.email());

        return "HH API доступен. Тип аккаунта: " + userType + ", email: " + email;
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank()
                ? "не указано"
                : value;
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

    private void setHhApiStatus(String message) {
        if (hhApiStatusLabel != null) {
            hhApiStatusLabel.setText(message);
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