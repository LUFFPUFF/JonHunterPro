package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.jobhunter.ui.navigation.UiNavigator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AuthenticatedUserDto;
import ru.jobhunter.core.application.usecase.user.LoginCommand;
import ru.jobhunter.core.application.usecase.user.LoginUseCase;
import ru.jobhunter.core.application.usecase.user.RegisterUserCommand;
import ru.jobhunter.core.application.usecase.user.RegisterUserUseCase;
import ru.jobhunter.ui.session.CurrentUserSession;

import java.util.concurrent.CompletionException;

@Component
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final CurrentUserSession currentUserSession;
    private final UiNavigator uiNavigator;

    @FXML
    private TextField loginEmailField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private TextField registerFullNameField;

    @FXML
    private TextField registerEmailField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private Label registerMessageLabel;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            CurrentUserSession currentUserSession,
            UiNavigator uiNavigator
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.currentUserSession = currentUserSession;
        this.uiNavigator = uiNavigator;
    }

    @FXML
    public void onLogin() {
        clearMessages();

        String email = loginEmailField.getText();
        String password = loginPasswordField.getText();

        loginMessageLabel.setText("Выполняется вход...");

        loginUseCase.execute(new LoginCommand(email, password))
                .thenAccept(user -> Platform.runLater(() -> handleSuccessfulAuth(user, loginMessageLabel)))
                .exceptionally(exception -> {
                    Platform.runLater(() -> handleAuthError(exception, loginMessageLabel));
                    return null;
                });
    }

    @FXML
    public void onRegister() {
        clearMessages();

        String fullName = registerFullNameField.getText();
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();

        registerMessageLabel.setText("Создаём аккаунт...");

        registerUserUseCase.execute(new RegisterUserCommand(email, password, fullName))
                .thenAccept(user -> Platform.runLater(() -> handleSuccessfulAuth(user, registerMessageLabel)))
                .exceptionally(exception -> {
                    Platform.runLater(() -> handleAuthError(exception, registerMessageLabel));
                    return null;
                });
    }

    private void handleSuccessfulAuth(AuthenticatedUserDto user, Label messageLabel) {
        currentUserSession.setCurrentUser(user);

        loginPasswordField.clear();
        registerPasswordField.clear();

        messageLabel.setText("Готово. Вы вошли как " + user.fullName());
        log.info("User authenticated: userId={}, email={}", user.id(), user.email());

        uiNavigator.showProfile();
    }

    private void handleAuthError(Throwable throwable, Label messageLabel) {
        Throwable rootCause = unwrap(throwable);

        log.warn("Authentication action failed: {}", rootCause.getMessage());

        messageLabel.setText(toUserFriendlyMessage(rootCause));
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }

        return throwable;
    }

    private String toUserFriendlyMessage(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return "Не удалось выполнить действие. Проверьте данные и попробуйте снова.";
        }

        if (message.contains("Invalid email or password")) {
            return "Неверная почта или пароль.";
        }

        if (message.contains("User already exists")) {
            return "Пользователь с такой почтой уже существует.";
        }

        if (message.contains("Password")) {
            return message;
        }

        if (message.contains("Email")) {
            return "Некорректный email.";
        }

        if (message.contains("Full name")) {
            return "Некорректное имя пользователя.";
        }

        return "Ошибка: " + message;
    }

    private void clearMessages() {
        loginMessageLabel.setText("");

    }
}
