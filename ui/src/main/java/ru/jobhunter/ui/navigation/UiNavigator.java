package ru.jobhunter.ui.navigation;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.AuthenticatedUserDto;
import ru.jobhunter.ui.loader.FxmlViewLoader;
import ru.jobhunter.ui.session.CurrentUserSession;

import java.util.Objects;

@Component
public class UiNavigator {

    private final FxmlViewLoader fxmlViewLoader;
    private final CurrentUserSession currentUserSession;

    private BorderPane rootPane;
    private Label statusLabel;

    public UiNavigator(
            FxmlViewLoader fxmlViewLoader,
            CurrentUserSession currentUserSession
    ) {
        this.fxmlViewLoader = Objects.requireNonNull(fxmlViewLoader, "FXML view loader must not be null");
        this.currentUserSession = Objects.requireNonNull(currentUserSession, "Current user session must not be null");
    }

    public void attach(BorderPane rootPane, Label statusLabel) {
        this.rootPane = Objects.requireNonNull(rootPane, "Root pane must not be null");
        this.statusLabel = Objects.requireNonNull(statusLabel, "Status label must not be null");
    }

    public void showAuth() {
        setCenter(fxmlViewLoader.load("/ru/jobhunter/ui/view/auth.fxml"));
        setStatus("Необходимо войти или зарегистрироваться.");
    }

    public void showProfile() {
        if (!currentUserSession.isAuthenticated()) {
            showAuth();
            return;
        }

        setCenter(fxmlViewLoader.load("/ru/jobhunter/ui/view/profile.fxml"));

        String fullName = currentUserSession.getCurrentUser()
                .map(AuthenticatedUserDto::fullName)
                .orElse("Пользователь");

        setStatus("Вы вошли как " + fullName);
    }

    public void showAutoResponses() {
        if (!currentUserSession.isAuthenticated()) {
            showAuth();
            return;
        }

        setCenter(fxmlViewLoader.load("/ru/jobhunter/ui/view/auto-responses.fxml"));
        setStatus("Автоотклики: поиск вакансий и подготовка очереди откликов.");
    }

    public void showPlaceholder(String title, String description) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(560);
        descriptionLabel.setStyle("-fx-font-size: 15px;");

        VBox box = new VBox(14, titleLabel, descriptionLabel);
        box.setStyle("-fx-alignment: center; -fx-padding: 32;");

        setCenter(box);
        setStatus(description);
    }

    private void setCenter(Node node) {
        ensureAttached();
        rootPane.setCenter(node);
    }

    private void setStatus(String message) {
        ensureAttached();
        statusLabel.setText(message);
    }

    private void ensureAttached() {
        if (rootPane == null || statusLabel == null) {
            throw new IllegalStateException("UiNavigator is not attached to main layout");
        }
    }
}
