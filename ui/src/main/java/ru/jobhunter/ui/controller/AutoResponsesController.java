package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.*;
import ru.jobhunter.core.application.usecase.autoresponse.*;
import ru.jobhunter.core.application.usecase.integration.SearchHhVacanciesUseCase;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.ui.session.CurrentUserSession;

import java.awt.Desktop;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public final class AutoResponsesController {

    private static final Logger log = LoggerFactory.getLogger(AutoResponsesController.class);

    private static final String PLATFORM_HH = "HH.ru";

    private static final DateTimeFormatter QUEUE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final SearchHhVacanciesUseCase searchHhVacanciesUseCase;
    private final CurrentUserSession currentUserSession;
    private final AddVacancyToAutoResponseQueueUseCase addVacancyToAutoResponseQueueUseCase;
    private final GetAutoResponseQueueUseCase getAutoResponseQueueUseCase;
    private final ExecuteAutoResponseUseCase executeAutoResponseUseCase;
    private final GetReadyAutoResponseQueueItemsUseCase getReadyAutoResponseQueueItemsUseCase;
    private final RemoveAutoResponseQueueItemUseCase removeAutoResponseQueueItemUseCase;
    private final UpdateAutoResponseQueueItemStatusUseCase updateAutoResponseQueueItemStatusUseCase;

    @FXML
    private ComboBox<String> platformComboBox;

    @FXML
    private TextField searchTextField;

    @FXML
    private TextField areaTextField;

    @FXML
    private Button searchButton;

    @FXML
    private Button openVacancyButton;

    @FXML
    private Button addToQueueButton;

    @FXML
    private Label searchStatusLabel;

    @FXML
    private Label resultCountLabel;

    @FXML
    private ListView<HhVacancyDto> vacanciesListView;

    @FXML
    private Button refreshQueueButton;

    @FXML
    private Button showReadyQueueButton;

    @FXML
    private Label queueStatusLabel;

    @FXML
    private ListView<AutoResponseQueueItemDto> queueListView;

    @FXML
    private Button executeAutoResponseButton;

    @FXML
    private Button removeQueueItemButton;

    @FXML
    private Button markQueueItemReadyButton;

    @FXML
    private Button returnQueueItemButton;

    @FXML
    private Button skipQueueItemButton;

    public AutoResponsesController(
            SearchHhVacanciesUseCase searchHhVacanciesUseCase,
            CurrentUserSession currentUserSession,
            AddVacancyToAutoResponseQueueUseCase addVacancyToAutoResponseQueueUseCase,
            GetAutoResponseQueueUseCase getAutoResponseQueueUseCase,
            GetReadyAutoResponseQueueItemsUseCase getReadyAutoResponseQueueItemsUseCase,
            ExecuteAutoResponseUseCase executeAutoResponseUseCase,
            RemoveAutoResponseQueueItemUseCase removeAutoResponseQueueItemUseCase,
            UpdateAutoResponseQueueItemStatusUseCase updateAutoResponseQueueItemStatusUseCase
    ) {
        this.searchHhVacanciesUseCase = Objects.requireNonNull(
                searchHhVacanciesUseCase,
                "Search HH vacancies use case must not be null"
        );
        this.currentUserSession = Objects.requireNonNull(
                currentUserSession,
                "Current user session must not be null"
        );
        this.addVacancyToAutoResponseQueueUseCase = Objects.requireNonNull(
                addVacancyToAutoResponseQueueUseCase,
                "Add vacancy to auto response queue use case must not be null"
        );
        this.getAutoResponseQueueUseCase = Objects.requireNonNull(
                getAutoResponseQueueUseCase,
                "Get auto response queue use case must not be null"
        );
        this.removeAutoResponseQueueItemUseCase = Objects.requireNonNull(
                removeAutoResponseQueueItemUseCase,
                "Remove auto response queue item use case must not be null"
        );
        this.updateAutoResponseQueueItemStatusUseCase = Objects.requireNonNull(
                updateAutoResponseQueueItemStatusUseCase,
                "Update auto response queue item status use case must not be null"
        );
        this.getReadyAutoResponseQueueItemsUseCase = Objects.requireNonNull(
                getReadyAutoResponseQueueItemsUseCase,
                "Get ready auto response queue items use case must not be null"
        );
        this.executeAutoResponseUseCase = Objects.requireNonNull(
                executeAutoResponseUseCase,
                "Execute auto response use case must not be null"
        );
    }

    @FXML
    public void initialize() {
        platformComboBox.getItems().setAll(PLATFORM_HH);
        platformComboBox.getSelectionModel().select(PLATFORM_HH);

        areaTextField.setText("113");

        vacanciesListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(HhVacancyDto vacancy, boolean empty) {
                super.updateItem(vacancy, empty);

                if (empty || vacancy == null) {
                    setText(null);
                    return;
                }

                setText(formatVacancy(vacancy));
            }
        });

        queueListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AutoResponseQueueItemDto item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(formatQueueItem(item));
            }
        });

        queueListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, selectedItem) ->
                        updateQueueActionButtons(selectedItem)
                );

        vacanciesListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, selectedVacancy) -> {
                    boolean vacancyNotSelected = selectedVacancy == null;
                    openVacancyButton.setDisable(vacancyNotSelected);
                    addToQueueButton.setDisable(vacancyNotSelected);
                });

        log.info("Auto responses screen initialized");

        loadQueue();
    }

    @FXML
    private void onRefreshQueueClicked() {
        loadQueue();
    }

    @FXML
    private void onShowReadyQueueClicked() {
        loadReadyQueue();
    }

    @FXML
    private void onMarkQueueItemReadyClicked() {
        updateSelectedQueueItemStatus(AutoResponseQueueStatus.READY);
    }

    @FXML
    private void onReturnQueueItemToQueuedClicked() {
        updateSelectedQueueItemStatus(AutoResponseQueueStatus.QUEUED);
    }

    @FXML
    private void onSkipQueueItemClicked() {
        updateSelectedQueueItemStatus(AutoResponseQueueStatus.SKIPPED);
    }

    @FXML
    private void onExecuteAutoResponseClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        AutoResponseQueueItemDto selectedItem = queueListView.getSelectionModel()
                .getSelectedItem();

        if (selectedItem == null) {
            setQueueStatus("Сначала выберите вакансию в очереди.");
            return;
        }

        if (selectedItem.status() != AutoResponseQueueStatus.READY) {
            setQueueStatus("Запуск автоотклика доступен только для вакансий со статусом «Готова к отклику».");
            return;
        }

        ExecuteAutoResponseCommand command = new ExecuteAutoResponseCommand(
                UserId.of(currentUser.id()),
                AutoResponseQueueItemId.of(selectedItem.id())
        );

        setQueueActionButtonsDisabled(true);
        setQueueStatus("Запускаем автоотклик: " + selectedItem.vacancyName());

        executeAutoResponseUseCase.execute(command)
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (throwable == null) {
                        setQueueStatus(
                                "Автоотклик завершен: "
                                        + selectedItem.vacancyName()
                                        + " → "
                                        + result.status()
                                        + formatExecutionMessage(result.message())
                        );

                        loadQueue();
                    } else {
                        updateQueueActionButtons(queueListView.getSelectionModel().getSelectedItem());
                        setQueueStatus("Не удалось выполнить автоотклик: " + rootMessage(throwable));
                    }
                }));
    }

    @FXML
    private void onSearchClicked() {
        String platform = platformComboBox.getSelectionModel().getSelectedItem();

        if (!PLATFORM_HH.equals(platform)) {
            setStatus("Сейчас поддерживается только поиск через HH.ru.");
            return;
        }

        String text = normalize(searchTextField.getText());
        String area = normalize(areaTextField.getText());

        if (text == null) {
            setStatus("Введите ключевые слова для поиска вакансий.");
            return;
        }

        HhVacancySearchQuery query;

        try {
            query = new HhVacancySearchQuery(
                    text,
                    area,
                    0,
                    20
            );
        } catch (IllegalArgumentException exception) {
            setStatus("Некорректные параметры поиска: " + exception.getMessage());
            return;
        }

        searchButton.setDisable(true);
        openVacancyButton.setDisable(true);
        addToQueueButton.setDisable(true);
        vacanciesListView.getItems().clear();
        resultCountLabel.setText("");
        setStatus("Ищем вакансии на HH.ru...");

        searchHhVacanciesUseCase.search(query)
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    searchButton.setDisable(false);

                    if (throwable == null) {
                        showSearchResult(result);
                    } else {
                        setStatus("Не удалось выполнить поиск HH.ru: " + rootMessage(throwable));
                    }
                }));
    }

    @FXML
    private void onOpenVacancyClicked() {
        HhVacancyDto vacancy = vacanciesListView.getSelectionModel()
                .getSelectedItem();

        if (vacancy == null) {
            setStatus("Сначала выберите вакансию.");
            return;
        }

        String url = firstNonBlank(
                vacancy.alternateUrl(),
                vacancy.url()
        );

        if (url == null) {
            setStatus("У выбранной вакансии нет ссылки для открытия.");
            return;
        }

        openBrowser(url);
    }

    @FXML
    private void onRemoveQueueItemClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        AutoResponseQueueItemDto selectedItem = queueListView.getSelectionModel()
                .getSelectedItem();

        if (selectedItem == null) {
            setQueueStatus("Сначала выберите вакансию в очереди.");
            return;
        }

        RemoveAutoResponseQueueItemCommand command = new RemoveAutoResponseQueueItemCommand(
                UserId.of(currentUser.id()),
                AutoResponseQueueItemId.of(selectedItem.id())
        );

        removeQueueItemButton.setDisable(true);
        setQueueStatus("Удаляем вакансию из очереди...");

        removeAutoResponseQueueItemUseCase.removeFromQueue(command)
                .whenComplete((deleted, throwable) -> Platform.runLater(() -> {
                    if (throwable == null) {
                        if (Boolean.TRUE.equals(deleted)) {
                            setQueueStatus("Вакансия удалена из очереди: " + selectedItem.vacancyName());
                        } else {
                            setQueueStatus("Вакансия уже отсутствует в очереди: " + selectedItem.vacancyName());
                        }

                        loadQueue();
                    } else {
                        removeQueueItemButton.setDisable(
                                queueListView.getSelectionModel().getSelectedItem() == null
                        );
                        setQueueStatus("Не удалось удалить вакансию из очереди: " + rootMessage(throwable));
                    }
                }));
    }

    @FXML
    private void onAddToQueueClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        HhVacancyDto vacancy = vacanciesListView.getSelectionModel()
                .getSelectedItem();

        if (vacancy == null) {
            setStatus("Сначала выберите вакансию.");
            return;
        }

        String vacancyUrl = firstNonBlank(
                vacancy.alternateUrl(),
                vacancy.url()
        );

        AddVacancyToAutoResponseQueueCommand command;

        try {
            command = new AddVacancyToAutoResponseQueueCommand(
                    UserId.of(currentUser.id()),
                    VacancySource.HH_RU,
                    vacancy.externalId(),
                    vacancy.name(),
                    vacancy.employerName(),
                    vacancy.areaName(),
                    vacancyUrl
            );
        } catch (IllegalArgumentException exception) {
            setStatus("Не удалось добавить вакансию в очередь: " + exception.getMessage());
            return;
        }

        addToQueueButton.setDisable(true);
        setStatus("Добавляем вакансию в очередь отклика...");

        addVacancyToAutoResponseQueueUseCase.addToQueue(command)
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    addToQueueButton.setDisable(
                            vacanciesListView.getSelectionModel().getSelectedItem() == null
                    );

                    if (throwable == null) {
                        if (result.created()) {
                            setStatus("Вакансия добавлена в очередь отклика: " + result.item().vacancyName());
                        } else {
                            setStatus("Эта вакансия уже есть в очереди отклика: " + result.item().vacancyName());
                        }

                        loadQueue();
                    } else {
                        setStatus("Не удалось добавить вакансию в очередь: " + rootMessage(throwable));
                    }
                }));
    }

    private void loadQueue() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        setQueueLoading(true);
        setQueueStatus("Загружаем очередь автооткликов...");

        getAutoResponseQueueUseCase.getQueue(UserId.of(currentUser.id()))
                .whenComplete((items, throwable) -> Platform.runLater(() -> {
                    setQueueLoading(false);

                    if (throwable == null) {
                        queueListView.getItems().setAll(items);
                        setQueueActionButtonsDisabled(true);

                        if (items.isEmpty()) {
                            setQueueStatus("Очередь автооткликов пока пуста.");
                        } else {
                            setQueueStatus("В очереди вакансий: " + items.size());
                        }
                    } else {
                        setQueueStatus("Не удалось загрузить очередь автооткликов: " + rootMessage(throwable));
                    }
                }));
    }

    private void updateSelectedQueueItemStatus(AutoResponseQueueStatus status) {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        AutoResponseQueueItemDto selectedItem = queueListView.getSelectionModel()
                .getSelectedItem();

        if (selectedItem == null) {
            setQueueStatus("Сначала выберите вакансию в очереди.");
            return;
        }

        UpdateAutoResponseQueueItemStatusCommand command = new UpdateAutoResponseQueueItemStatusCommand(
                UserId.of(currentUser.id()),
                AutoResponseQueueItemId.of(selectedItem.id()),
                status
        );

        setQueueActionButtonsDisabled(true);
        setQueueStatus("Обновляем статус вакансии...");

        updateAutoResponseQueueItemStatusUseCase.updateStatus(command)
                .whenComplete((updatedItem, throwable) -> Platform.runLater(() -> {
                    if (throwable == null) {
                        if (updatedItem.isPresent()) {
                            AutoResponseQueueItemDto item = updatedItem.get();

                            setQueueStatus(
                                    "Статус обновлен: "
                                            + item.vacancyName()
                                            + " → "
                                            + formatQueueStatus(item.status())
                            );
                        } else {
                            setQueueStatus("Вакансия уже отсутствует в очереди: " + selectedItem.vacancyName());
                        }

                        loadQueue();
                    } else {
                        updateQueueActionButtons(queueListView.getSelectionModel().getSelectedItem());
                        setQueueStatus("Не удалось обновить статус вакансии: " + rootMessage(throwable));
                    }
                }));
    }

    private void loadReadyQueue() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        setQueueLoading(true);
        setQueueStatus("Загружаем вакансии, готовые к отклику...");

        getReadyAutoResponseQueueItemsUseCase.getReadyItems(UserId.of(currentUser.id()))
                .whenComplete((items, throwable) -> Platform.runLater(() -> {
                    setQueueLoading(false);

                    if (throwable == null) {
                        queueListView.getItems().setAll(items);
                        setQueueActionButtonsDisabled(true);

                        if (items.isEmpty()) {
                            setQueueStatus("Нет вакансий со статусом «Готова к отклику».");
                        } else {
                            setQueueStatus("Готовы к отклику: " + items.size());
                        }
                    } else {
                        setQueueStatus("Не удалось загрузить готовые к отклику вакансии: " + rootMessage(throwable));
                    }
                }));
    }

    private void showSearchResult(HhVacancySearchResultDto result) {
        vacanciesListView.getItems().setAll(result.vacancies());

        resultCountLabel.setText("Найдено: " + result.found());

        if (result.vacancies().isEmpty()) {
            setStatus("Поиск завершен. Вакансии не найдены.");
        } else {
            setStatus("Поиск завершен. Показано вакансий: " + result.vacancies().size());
        }
    }

    private String formatVacancy(HhVacancyDto vacancy) {
        return vacancy.name()
                + "\nПлатформа: HH.ru"
                + "\nКомпания: " + valueOrUnknown(vacancy.employerName())
                + "\nРегион: " + valueOrUnknown(vacancy.areaName())
                + "\nЗарплата: " + formatSalary(vacancy.salary())
                + "\nОпыт: " + valueOrUnknown(vacancy.experienceName())
                + "\nГрафик: " + valueOrUnknown(vacancy.scheduleName());
    }

    private String formatQueueItem(AutoResponseQueueItemDto item) {
        return item.vacancyName()
                + "\nПлатформа: " + item.source()
                + "\nКомпания: " + valueOrUnknown(item.employerName())
                + "\nРегион: " + valueOrUnknown(item.areaName())
                + "\nСтатус: " + formatQueueStatus(item.status())
                + "\nДобавлена: " + QUEUE_DATE_FORMATTER.format(item.createdAt());
    }

    private void setQueueStatus(String message) {
        queueStatusLabel.setText(message);
    }

    private void setQueueLoading(boolean loading) {
        refreshQueueButton.setDisable(loading);
        showReadyQueueButton.setDisable(loading);
        setQueueActionButtonsDisabled(loading);
    }

    private void updateQueueActionButtons(AutoResponseQueueItemDto selectedItem) {
        if (selectedItem == null) {
            setQueueActionButtonsDisabled(true);
            return;
        }

        AutoResponseQueueStatus status = selectedItem.status();

        removeQueueItemButton.setDisable(false);

        executeAutoResponseButton.setDisable(status != AutoResponseQueueStatus.READY);

        markQueueItemReadyButton.setDisable(
                status == AutoResponseQueueStatus.READY
                        || status == AutoResponseQueueStatus.SENT
        );

        returnQueueItemButton.setDisable(
                status == AutoResponseQueueStatus.QUEUED
                        || status == AutoResponseQueueStatus.SENT
        );

        skipQueueItemButton.setDisable(
                status == AutoResponseQueueStatus.SKIPPED
                        || status == AutoResponseQueueStatus.SENT
        );
    }

    private void setQueueActionButtonsDisabled(boolean disabled) {
        executeAutoResponseButton.setDisable(disabled);
        removeQueueItemButton.setDisable(disabled);
        markQueueItemReadyButton.setDisable(disabled);
        returnQueueItemButton.setDisable(disabled);
        skipQueueItemButton.setDisable(disabled);
    }

    private String formatQueueStatus(AutoResponseQueueStatus status) {
        if (status == null) {
            return "не указан";
        }

        return switch (status) {
            case QUEUED -> "В очереди";
            case READY -> "Готова к отклику";
            case SENT -> "Отклик отправлен";
            case FAILED -> "Ошибка отправки";
            case SKIPPED -> "Пропущена";
        };
    }

    private String formatSalary(HhSalaryDto salary) {
        if (salary == null) {
            return "не указана";
        }

        StringBuilder builder = new StringBuilder();

        if (salary.from() != null) {
            builder.append("от ").append(salary.from());
        }

        if (salary.to() != null) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            builder.append("до ").append(salary.to());
        }

        if (builder.isEmpty()) {
            builder.append("не указана");
        }

        if (salary.currency() != null && !salary.currency().isBlank()) {
            builder.append(" ").append(salary.currency());
        }

        if (Boolean.TRUE.equals(salary.gross())) {
            builder.append(", до вычета налогов");
        }

        return builder.toString();
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
            setStatus("Не удалось открыть вакансию в браузере: " + rootMessage(exception));
        }
    }

    private void setStatus(String message) {
        searchStatusLabel.setText(message);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank()
                ? "не указано"
                : value;
    }

    private String formatExecutionMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        return ". " + message;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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