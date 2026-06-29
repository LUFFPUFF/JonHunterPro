package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
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
import ru.jobhunter.core.application.usecase.resume.GetPrimaryResumeUseCase;
import ru.jobhunter.core.application.usecase.resume.UploadPrimaryResumePdfUseCase;
import ru.jobhunter.core.domain.model.AutoResponseQueueItemId;
import ru.jobhunter.core.domain.model.AutoResponseQueueStatus;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.ui.session.CurrentUserSession;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.control.TextArea;
import ru.jobhunter.core.application.usecase.coverletter.GetGeneralCoverLetterSettingsUseCase;
import ru.jobhunter.core.application.usecase.coverletter.SaveGeneralCoverLetterSettingsUseCase;

import java.nio.file.Path;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public final class AutoResponsesController {

    private static final Logger log = LoggerFactory.getLogger(AutoResponsesController.class);

    private static final String PLATFORM_HH = "HH.ru";
    private static final int DEFAULT_SEARCH_PAGE_SIZE = 20;

    private static final DateTimeFormatter QUEUE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final SearchHhVacanciesUseCase searchHhVacanciesUseCase;
    private final CurrentUserSession currentUserSession;
    private final AddVacancyToAutoResponseQueueUseCase addVacancyToAutoResponseQueueUseCase;
    private final AddVacanciesToAutoResponseQueueUseCase addVacanciesToAutoResponseQueueUseCase;
    private final GetGeneralCoverLetterSettingsUseCase getGeneralCoverLetterSettingsUseCase;
    private final SaveGeneralCoverLetterSettingsUseCase saveGeneralCoverLetterSettingsUseCase;
    private final GetAutoResponseQueueUseCase getAutoResponseQueueUseCase;
    private final ExecuteAutoResponseUseCase executeAutoResponseUseCase;
    private final GetReadyAutoResponseQueueItemsUseCase getReadyAutoResponseQueueItemsUseCase;

    private final RemoveAutoResponseQueueItemUseCase removeAutoResponseQueueItemUseCase;
    private final UpdateAutoResponseQueueItemStatusUseCase updateAutoResponseQueueItemStatusUseCase;
    private final MarkAutoResponseQueueItemsReadyUseCase markAutoResponseQueueItemsReadyUseCase;
    private final GetPrimaryResumeUseCase getPrimaryResumeUseCase;
    private final UploadPrimaryResumePdfUseCase uploadPrimaryResumePdfUseCase;
    private final StartReadyAutoResponsesBatchUseCase startReadyAutoResponsesBatchUseCase;
    private final GetAutoResponseBatchProgressUseCase getAutoResponseBatchProgressUseCase;
    private String activeSearchText;
    private String activeSearchArea;
    private int currentSearchPage;
    private int currentSearchPerPage = DEFAULT_SEARCH_PAGE_SIZE;
    private HhVacancySearchResultDto lastSearchResult;
    private boolean searchLoading;
    private final Set<String> selectedVacancyExternalIds = new LinkedHashSet<>();
    private boolean updatingSelectAllVacancies;
    private boolean queueAdditionLoading;
    private final Set<UUID> selectedQueuedQueueItemIds = new LinkedHashSet<>();
    private boolean updatingSelectAllQueuedQueueItems;
    private boolean queueReadyBatchLoading;
    private boolean generalCoverLetterLoading;
    private boolean replacingGeneralCoverLetterContent;

    private String generalCoverLetterSourceFileName;

    private Timeline autoResponseBatchProgressTimeline;

    private UUID activeAutoResponseBatchId;

    private boolean autoResponseBatchStartLoading;

    private int lastObservedBatchStartedCount = -1;

    private int lastObservedBatchProcessedCount = -1;


    @FXML
    private Label resumeStatusLabel;

    @FXML
    private TextArea generalCoverLetterTextArea;

    @FXML
    private CheckBox useGeneralCoverLetterWhenLlmUnavailableCheckBox;

    @FXML
    private Button loadGeneralCoverLetterTextButton;

    @FXML
    private Button saveGeneralCoverLetterButton;

    @FXML
    private Label generalCoverLetterStatusLabel;

    @FXML
    private Button resumeUploadPdfButton;

    @FXML
    private ComboBox<String> platformComboBox;

    @FXML
    private TextField searchTextField;

    @FXML
    private TextField areaTextField;

    @FXML
    private Button searchButton;

    @FXML
    private Button previousPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Label paginationLabel;

    @FXML
    private ComboBox<Integer> pageSizeComboBox;

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
    private CheckBox selectAllVacanciesCheckBox;

    @FXML
    private Label selectedVacancyCountLabel;

    @FXML
    private Button refreshQueueButton;

    @FXML
    private Button showReadyQueueButton;

    @FXML
    private Button startReadyAutoResponsesBatchButton;

    @FXML
    private Label batchProgressLabel;

    @FXML
    private Label queueStatusLabel;

    @FXML
    private ListView<AutoResponseQueueItemDto> queueListView;

    @FXML
    private CheckBox selectAllQueuedQueueItemsCheckBox;

    @FXML
    private Label selectedQueuedQueueItemCountLabel;

    @FXML
    private Button markSelectedQueueItemsReadyButton;

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

    @FXML
    private Button copyQueueVacancyIdButton;

    @FXML
    private Button openQueueVacancyButton;

    @FXML
    private Button openQueueDiagnosticsButton;

    public AutoResponsesController(
            SearchHhVacanciesUseCase searchHhVacanciesUseCase,
            CurrentUserSession currentUserSession,
            AddVacancyToAutoResponseQueueUseCase addVacancyToAutoResponseQueueUseCase,
            AddVacanciesToAutoResponseQueueUseCase addVacanciesToAutoResponseQueueUseCase,
            GetAutoResponseQueueUseCase getAutoResponseQueueUseCase,
            GetReadyAutoResponseQueueItemsUseCase getReadyAutoResponseQueueItemsUseCase,
            StartReadyAutoResponsesBatchUseCase startReadyAutoResponsesBatchUseCase,
            GetAutoResponseBatchProgressUseCase getAutoResponseBatchProgressUseCase,
            ExecuteAutoResponseUseCase executeAutoResponseUseCase,
            RemoveAutoResponseQueueItemUseCase removeAutoResponseQueueItemUseCase,
            UpdateAutoResponseQueueItemStatusUseCase updateAutoResponseQueueItemStatusUseCase,
            MarkAutoResponseQueueItemsReadyUseCase markAutoResponseQueueItemsReadyUseCase,
            GetPrimaryResumeUseCase getPrimaryResumeUseCase,
            UploadPrimaryResumePdfUseCase uploadPrimaryResumePdfUseCase,
            GetGeneralCoverLetterSettingsUseCase getGeneralCoverLetterSettingsUseCase,
            SaveGeneralCoverLetterSettingsUseCase saveGeneralCoverLetterSettingsUseCase
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
        this.addVacanciesToAutoResponseQueueUseCase = Objects.requireNonNull(
                addVacanciesToAutoResponseQueueUseCase,
                "Add vacancies to auto response queue use case must not be null"
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
        this.markAutoResponseQueueItemsReadyUseCase =
                Objects.requireNonNull(
                        markAutoResponseQueueItemsReadyUseCase,
                        "Mark auto response queue items ready use case "
                                + "must not be null"
                );
        this.getReadyAutoResponseQueueItemsUseCase = Objects.requireNonNull(
                getReadyAutoResponseQueueItemsUseCase,
                "Get ready auto response queue items use case must not be null"
        );
        this.startReadyAutoResponsesBatchUseCase =
                Objects.requireNonNull(
                        startReadyAutoResponsesBatchUseCase,
                        "Start ready auto responses batch use case "
                                + "must not be null"
                );

        this.getAutoResponseBatchProgressUseCase =
                Objects.requireNonNull(
                        getAutoResponseBatchProgressUseCase,
                        "Get auto response batch progress use case "
                                + "must not be null"
                );
        this.executeAutoResponseUseCase = Objects.requireNonNull(
                executeAutoResponseUseCase,
                "Execute auto response use case must not be null"
        );
        this.getPrimaryResumeUseCase = Objects.requireNonNull(
                getPrimaryResumeUseCase,
                "Get primary resume use case must not be null"
        );

        this.uploadPrimaryResumePdfUseCase = Objects.requireNonNull(
                uploadPrimaryResumePdfUseCase,
                "Upload primary resume PDF use case must not be null"
        );
        this.getGeneralCoverLetterSettingsUseCase = Objects.requireNonNull(
                getGeneralCoverLetterSettingsUseCase,
                "Get general cover letter settings use case must not be null"
        );

        this.saveGeneralCoverLetterSettingsUseCase = Objects.requireNonNull(
                saveGeneralCoverLetterSettingsUseCase,
                "Save general cover letter settings use case must not be null"
        );
    }

    @FXML
    public void initialize() {
        platformComboBox.getItems().setAll(PLATFORM_HH);
        platformComboBox.getSelectionModel().select(PLATFORM_HH);

        pageSizeComboBox.getItems().setAll(20, 50, 100);
        pageSizeComboBox.getSelectionModel()
                .select(Integer.valueOf(DEFAULT_SEARCH_PAGE_SIZE));

        selectAllVacanciesCheckBox.selectedProperty()
                .addListener((observable, oldValue, selected) -> {
                    if (!updatingSelectAllVacancies) {
                        setCurrentPageVacanciesSelected(selected);
                    }
                });

        areaTextField.setText("113");

        vacanciesListView.setCellFactory(
                listView -> new VacancySelectionListCell()
        );

        queueListView.setCellFactory(
                listView -> new QueueSelectionListCell()
        );

        queueListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, selectedItem) ->
                        updateQueueActionButtons(selectedItem)
                );

        vacanciesListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, selectedVacancy) ->
                        updateSearchControls()
                );

        selectAllQueuedQueueItemsCheckBox.selectedProperty()
                .addListener((observable, oldValue, selected) -> {
                    if (!updatingSelectAllQueuedQueueItems) {
                        setCurrentQueueItemsSelectedForReady(selected);
                    }
                });

        generalCoverLetterTextArea.textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (!replacingGeneralCoverLetterContent) {
                        generalCoverLetterSourceFileName = null;
                    }
                });

        log.info("Auto responses screen initialized");

        updateSearchControls();
        refreshQueueSelectionUi();
        autoResponseBatchProgressTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> refreshAutoResponseBatchProgress()
                )
        );

        autoResponseBatchProgressTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        refreshBatchStartButton();
        loadQueue();
        loadPrimaryResumeStatus();
        loadGeneralCoverLetterSettings();
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
    private void onStartReadyAutoResponsesBatchClicked() {
        if (isAutoResponseBatchActive()
                || autoResponseBatchStartLoading) {
            setQueueStatus(
                    "Массовый запуск уже выполняется."
            );
            return;
        }

        AuthenticatedUserDto currentUser = currentUserSession
                .getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus(
                    "Сначала войдите в аккаунт JobHunterPro."
            );
            return;
        }

        autoResponseBatchStartLoading = true;
        refreshBatchStartButton();

        batchProgressLabel.setText(
                "Проверяем основное резюме перед массовым запуском..."
        );

        getPrimaryResumeUseCase.getPrimaryResume(
                        UserId.of(currentUser.id())
                )
                .whenComplete((optionalResume, throwable) ->
                        Platform.runLater(() -> {
                            if (throwable != null) {
                                autoResponseBatchStartLoading = false;
                                refreshBatchStartButton();

                                batchProgressLabel.setText(
                                        "Массовый запуск не начат: "
                                                + rootMessage(throwable)
                                );

                                return;
                            }

                            if (optionalResume.isEmpty()) {
                                autoResponseBatchStartLoading = false;
                                refreshBatchStartButton();

                                batchProgressLabel.setText(
                                        "Массовый запуск не начат: "
                                                + "сначала загрузите основное "
                                                + "резюме в PDF."
                                );

                                return;
                            }

                            startReadyAutoResponsesBatch(
                                    currentUser
                            );
                        })
                );
    }

    @FXML
    private void onMarkQueueItemReadyClicked() {
        updateSelectedQueueItemStatus(AutoResponseQueueStatus.READY);
    }

    @FXML
    private void onMarkSelectedQueueItemsReadyClicked() {
        AuthenticatedUserDto currentUser = currentUserSession
                .getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        List<AutoResponseQueueItemDto> selectedItems =
                getSelectedQueuedQueueItems();

        if (selectedItems.isEmpty()) {
            setQueueStatus(
                    "Отметьте хотя бы одну вакансию "
                            + "со статусом «В очереди»."
            );
            return;
        }

        List<AutoResponseQueueItemId> itemIds = selectedItems.stream()
                .map(AutoResponseQueueItemDto::id)
                .map(AutoResponseQueueItemId::of)
                .toList();

        MarkAutoResponseQueueItemsReadyCommand command =
                new MarkAutoResponseQueueItemsReadyCommand(
                        UserId.of(currentUser.id()),
                        itemIds
                );

        setQueueReadyBatchLoading(true);

        setQueueStatus(
                "Помечаем готовыми вакансии: "
                        + selectedItems.size()
                        + "..."
        );

        markAutoResponseQueueItemsReadyUseCase.markReady(command)
                .whenComplete((result, throwable) ->
                        Platform.runLater(() -> {
                            setQueueReadyBatchLoading(false);

                            if (throwable != null) {
                                setQueueStatus(
                                        "Не удалось массово обновить статусы: "
                                                + rootMessage(throwable)
                                );
                                return;
                            }

                            setQueueStatus(
                                    formatBatchReadyResult(result)
                            );

                            loadQueue();
                        })
                );
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

        if (isAutoResponseBatchActive()) {
            setQueueStatus(
                    "Сейчас выполняется массовый запуск. "
                            + "Дождитесь его завершения."
            );
            return;
        }

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
            setQueueStatus(
                    "Запуск автоотклика доступен только для вакансий "
                            + "со статусом «Готова к отклику»."
            );
            return;
        }

        setQueueActionButtonsDisabled(true);
        setQueueStatus("Проверяем основное резюме перед автооткликом...");

        getPrimaryResumeUseCase.getPrimaryResume(UserId.of(currentUser.id()))
                .whenComplete((optionalResume, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        updateQueueActionButtons(
                                queueListView.getSelectionModel().getSelectedItem()
                        );

                        setQueueStatus(
                                "Не удалось проверить основное резюме: "
                                        + rootMessage(throwable)
                        );
                        return;
                    }

                    if (optionalResume.isEmpty()) {
                        updateQueueActionButtons(
                                queueListView.getSelectionModel().getSelectedItem()
                        );

                        setQueueStatus(
                                "Автоотклик не запущен: сначала загрузите основное "
                                        + "резюме в PDF. Оно необходимо для генерации "
                                        + "сопроводительного письма."
                        );
                        return;
                    }

                    executeAutoResponse(currentUser, selectedItem);
                }));
    }

    @FXML
    private void onLoadGeneralCoverLetterTextClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser().orElse(null);
        if (currentUser == null) {
            setGeneralCoverLetterStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите общее сопроводительное письмо");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt"));
        Window ownerWindow = loadGeneralCoverLetterTextButton.getScene() == null ? null : loadGeneralCoverLetterTextButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile == null) {
            return;
        }
        try {
            String content = GeneralCoverLetterTextFileReader.read(selectedFile.toPath());
            replaceGeneralCoverLetterContent(content);
            generalCoverLetterSourceFileName = selectedFile.getName();
            setGeneralCoverLetterStatus("Текст загружен из файла «" + selectedFile.getName() + "». Нажмите «Сохранить письмо».");
        } catch (IOException | IllegalArgumentException exception) {
            setGeneralCoverLetterStatus("Не удалось загрузить TXT-файл: " + rootMessage(exception));
        }
    }

    @FXML
    private void onSaveGeneralCoverLetterClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser().orElse(null);
        if (currentUser == null) {
            setGeneralCoverLetterStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }
        String content = generalCoverLetterTextArea.getText();
        if (content == null || content.isBlank()) {
            setGeneralCoverLetterStatus("Введите или загрузите текст сопроводительного письма.");
            return;
        }
        SaveGeneralCoverLetterSettingsCommand command = new SaveGeneralCoverLetterSettingsCommand(UserId.of(currentUser.id()), content, useGeneralCoverLetterWhenLlmUnavailableCheckBox.isSelected(), generalCoverLetterSourceFileName);
        setGeneralCoverLetterLoading(true);
        setGeneralCoverLetterStatus("Сохраняем общее сопроводительное письмо...");
        saveGeneralCoverLetterSettingsUseCase.save(command).whenComplete((savedSettings, throwable) -> Platform.runLater(() -> {
            setGeneralCoverLetterLoading(false);
            if (throwable != null) {
                setGeneralCoverLetterStatus("Не удалось сохранить письмо: " + rootMessage(throwable));
                return;
            }
            replaceGeneralCoverLetterContent(savedSettings.content());
            generalCoverLetterSourceFileName = savedSettings.sourceFileName();
            useGeneralCoverLetterWhenLlmUnavailableCheckBox.setSelected(savedSettings.useWhenLlmUnavailable());
            setGeneralCoverLetterStatus("Общее письмо сохранено: " + savedSettings.content().length() + " символов. Fallback при " + "недоступности LLM: " + (savedSettings.useWhenLlmUnavailable() ? "включён" : "выключен") + ".");
        }));
    }

    @FXML
    private void onUploadResumePdfClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setQueueStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите резюме в формате PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "PDF-документы (*.pdf)",
                        "*.pdf"
                )
        );

        Window ownerWindow = resumeUploadPdfButton.getScene() == null
                ? null
                : resumeUploadPdfButton.getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(ownerWindow);

        if (selectedFile == null) {
            return;
        }

        byte[] pdfBytes;

        try {
            pdfBytes = Files.readAllBytes(selectedFile.toPath());
        } catch (IOException exception) {
            setQueueStatus(
                    "Не удалось прочитать выбранный PDF-файл: "
                            + rootMessage(exception)
            );
            return;
        }

        UploadPrimaryResumePdfCommand command;

        try {
            command = new UploadPrimaryResumePdfCommand(
                    UserId.of(currentUser.id()),
                    selectedFile.getName(),
                    pdfBytes
            );
        } catch (IllegalArgumentException exception) {
            setQueueStatus(
                    "Не удалось загрузить резюме: "
                            + exception.getMessage()
            );
            return;
        }

        resumeUploadPdfButton.setDisable(true);
        setQueueStatus("Извлекаем текст из PDF-резюме...");

        uploadPrimaryResumePdfUseCase.uploadPdf(command)
                .whenComplete((resume, throwable) -> Platform.runLater(() -> {
                    resumeUploadPdfButton.setDisable(false);

                    if (throwable == null) {
                        setQueueStatus(
                                "Основное резюме сохранено: "
                                        + resume.title()
                                        + ". Оно будет использовано при следующем автоотклике."
                        );

                        loadPrimaryResumeStatus();
                    } else {
                        setQueueStatus(
                                "Не удалось загрузить PDF-резюме: "
                                        + rootMessage(throwable)
                        );
                    }
                }));
    }

    @FXML
    private void onSearchClicked() {
        String platform = platformComboBox.getSelectionModel()
                .getSelectedItem();

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

        loadSearchPage(
                text,
                area,
                0,
                selectedPageSize()
        );
    }

    @FXML
    private void onPreviousPageClicked() {
        if (lastSearchResult == null
                || !lastSearchResult.hasPreviousPage()
                || activeSearchText == null) {
            return;
        }

        loadSearchPage(
                activeSearchText,
                activeSearchArea,
                lastSearchResult.page() - 1,
                currentSearchPerPage
        );
    }

    @FXML
    private void onNextPageClicked() {
        if (lastSearchResult == null
                || !lastSearchResult.hasNextPage()
                || activeSearchText == null) {
            return;
        }

        loadSearchPage(
                activeSearchText,
                activeSearchArea,
                lastSearchResult.page() + 1,
                currentSearchPerPage
        );
    }

    @FXML
    private void onPageSizeChanged() {
        if (searchLoading || activeSearchText == null) {
            return;
        }

        int selectedPerPage = selectedPageSize();

        if (selectedPerPage == currentSearchPerPage) {
            return;
        }

        loadSearchPage(
                activeSearchText,
                activeSearchArea,
                0,
                selectedPerPage
        );
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
    private void onCopyQueueVacancyIdClicked() {
        AutoResponseQueueItemDto item = queueListView
                .getSelectionModel()
                .getSelectedItem();

        if (item == null) {
            setQueueStatus("Сначала выберите вакансию в очереди.");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(item.externalVacancyId());

        Clipboard.getSystemClipboard()
                .setContent(content);

        setQueueStatus(
                "ID HH скопирован: "
                        + item.externalVacancyId()
        );
    }

    @FXML
    private void onOpenQueueVacancyClicked() {
        AutoResponseQueueItemDto item = queueListView
                .getSelectionModel()
                .getSelectedItem();

        if (item == null) {
            setQueueStatus("Сначала выберите вакансию в очереди.");
            return;
        }

        String vacancyUrl = firstNonBlank(
                item.vacancyUrl(),
                "https://hh.ru/vacancy/"
                        + item.externalVacancyId()
        );

        openQueueBrowser(vacancyUrl);
    }

    @FXML
    private void onOpenQueueDiagnosticsClicked() {
        AutoResponseQueueItemDto item = queueListView
                .getSelectionModel()
                .getSelectedItem();

        if (item == null
                || item.diagnosticDirectory() == null
                || item.diagnosticDirectory().isBlank()) {
            setQueueStatus(
                    "Для выбранной вакансии нет сохранённой диагностики."
            );
            return;
        }

        Path diagnosticsDirectory = Path.of(
                item.diagnosticDirectory()
        ).toAbsolutePath().normalize();

        if (!Files.isDirectory(diagnosticsDirectory)) {
            setQueueStatus(
                    "Папка diagnostics не найдена: "
                            + diagnosticsDirectory
            );
            return;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException(
                        "Открытие папок не поддерживается"
                );
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                throw new IllegalStateException(
                        "Открытие папок не поддерживается"
                );
            }

            desktop.open(diagnosticsDirectory.toFile());

            setQueueStatus(
                    "Открыта диагностика вакансии "
                            + item.externalVacancyId()
            );
        } catch (Exception exception) {
            setQueueStatus(
                    "Не удалось открыть diagnostics: "
                            + rootMessage(exception)
            );
        }
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

        List<HhVacancyDto> selectedVacancies =
                getSelectedCurrentPageVacancies();

        if (selectedVacancies.isEmpty()) {
            setStatus(
                    "Отметьте хотя бы одну вакансию "
                            + "для добавления в очередь."
            );
            return;
        }

        List<AddVacancyToAutoResponseQueueCommand> commands =
                new ArrayList<>(selectedVacancies.size());

        try {
            for (HhVacancyDto vacancy : selectedVacancies) {
                commands.add(
                        toQueueCommand(
                                currentUser,
                                vacancy
                        )
                );
            }

            AddVacanciesToAutoResponseQueueCommand command =
                    new AddVacanciesToAutoResponseQueueCommand(
                            UserId.of(currentUser.id()),
                            commands
                    );

            setQueueAdditionLoading(true);

            setStatus(
                    "Добавляем в очередь вакансии: "
                            + selectedVacancies.size()
                            + "..."
            );

            addVacanciesToAutoResponseQueueUseCase.addAllToQueue(command)
                    .whenComplete((result, throwable) ->
                            Platform.runLater(() -> {
                                setQueueAdditionLoading(false);

                                if (throwable != null) {
                                    setStatus(
                                            "Не удалось добавить вакансии в очередь: "
                                                    + rootMessage(throwable)
                                    );
                                    return;
                                }

                                retainOnlyFailedVacancySelections(result);

                                setStatus(
                                        formatBatchQueueAdditionResult(result)
                                );

                                loadQueue();
                            })
                    );
        } catch (IllegalArgumentException exception) {
            setStatus(
                    "Не удалось подготовить вакансии для очереди: "
                            + exception.getMessage()
            );
        }
    }

    private List<HhVacancyDto> getSelectedCurrentPageVacancies() {
        return vacanciesListView.getItems()
                .stream()
                .filter(this::isVacancySelected)
                .toList();
    }

    private AddVacancyToAutoResponseQueueCommand toQueueCommand(
            AuthenticatedUserDto currentUser,
            HhVacancyDto vacancy
    ) {
        String vacancyUrl = firstNonBlank(
                vacancy.alternateUrl(),
                vacancy.url()
        );

        return new AddVacancyToAutoResponseQueueCommand(
                UserId.of(currentUser.id()),
                VacancySource.HH_RU,
                vacancy.externalId(),
                vacancy.name(),
                vacancy.employerName(),
                vacancy.areaName(),
                vacancyUrl
        );
    }

    private void retainOnlyFailedVacancySelections(
            AddVacanciesToAutoResponseQueueResultDto result
    ) {
        Set<String> failedVacancyIds = new LinkedHashSet<>();

        for (AddVacancyToAutoResponseQueueFailureDto failure
                : result.failures()) {
            failedVacancyIds.add(failure.externalVacancyId());
        }

        selectedVacancyExternalIds.retainAll(
                failedVacancyIds
        );

        refreshVacancySelectionUi();
    }

    private String formatBatchQueueAdditionResult(
            AddVacanciesToAutoResponseQueueResultDto result
    ) {
        String message = "Обработка завершена. Добавлено: "
                + result.addedCount()
                + ". Уже в очереди: "
                + result.alreadyExistsCount()
                + ". Ошибок: "
                + result.failedCount()
                + ".";

        if (result.failures().isEmpty()) {
            return message;
        }

        AddVacancyToAutoResponseQueueFailureDto firstFailure =
                result.failures().getFirst();

        return message
                + " Первая ошибка: "
                + firstFailure.vacancyName()
                + " — "
                + firstFailure.message();
    }

    private void setQueueAdditionLoading(
            boolean loading
    ) {
        queueAdditionLoading = loading;

        updateSearchControls();
        vacanciesListView.refresh();
    }

    private List<AutoResponseQueueItemDto> getSelectedQueuedQueueItems() {
        return queueListView.getItems()
                .stream()
                .filter(item ->
                        item.status() == AutoResponseQueueStatus.QUEUED
                )
                .filter(this::isQueueItemSelectedForReady)
                .toList();
    }

    private void setCurrentQueueItemsSelectedForReady(
            boolean selected
    ) {
        if (queueReadyBatchLoading) {
            return;
        }

        for (AutoResponseQueueItemDto item : queueListView.getItems()) {
            if (item.status() == AutoResponseQueueStatus.QUEUED) {
                updateQueueItemReadySelection(
                        item,
                        selected,
                        false
                );
            }
        }

        refreshQueueSelectionUi();
    }

    private void updateQueueItemReadySelection(
            AutoResponseQueueItemDto item,
            boolean selected,
            boolean refreshUi
    ) {
        if (item == null
                || item.status() != AutoResponseQueueStatus.QUEUED) {
            return;
        }

        if (selected) {
            selectedQueuedQueueItemIds.add(item.id());
        } else {
            selectedQueuedQueueItemIds.remove(item.id());
        }

        if (refreshUi) {
            refreshQueueSelectionUi();
        }
    }

    private boolean isQueueItemSelectedForReady(
            AutoResponseQueueItemDto item
    ) {
        return item != null
                && item.status() == AutoResponseQueueStatus.QUEUED
                && selectedQueuedQueueItemIds.contains(item.id());
    }

    private void clearQueueReadySelection() {
        selectedQueuedQueueItemIds.clear();

        updatingSelectAllQueuedQueueItems = true;
        selectAllQueuedQueueItemsCheckBox.setSelected(false);
        selectAllQueuedQueueItemsCheckBox.setIndeterminate(false);
        updatingSelectAllQueuedQueueItems = false;

        selectedQueuedQueueItemCountLabel.setText("Выбрано: 0");
    }

    private void refreshQueueSelectionUi() {
        List<AutoResponseQueueItemDto> queuedItems = queueListView.getItems()
                .stream()
                .filter(item ->
                        item.status() == AutoResponseQueueStatus.QUEUED
                )
                .toList();

        int selectedCount = (int) queuedItems.stream()
                .filter(this::isQueueItemSelectedForReady)
                .count();

        boolean allSelected = !queuedItems.isEmpty()
                && selectedCount == queuedItems.size();

        boolean partiallySelected = selectedCount > 0
                && !allSelected;

        updatingSelectAllQueuedQueueItems = true;
        selectAllQueuedQueueItemsCheckBox.setSelected(allSelected);
        selectAllQueuedQueueItemsCheckBox.setIndeterminate(
                partiallySelected
        );
        updatingSelectAllQueuedQueueItems = false;

        boolean queueSelectionLocked = queueReadyBatchLoading
                || isAutoResponseBatchActive();

        selectAllQueuedQueueItemsCheckBox.setDisable(
                queueSelectionLocked || queuedItems.isEmpty()
        );

        markSelectedQueueItemsReadyButton.setDisable(
                queueSelectionLocked || selectedCount == 0
        );

        selectedQueuedQueueItemCountLabel.setText(
                "Выбрано: " + selectedCount
        );

        queueListView.refresh();
    }

    private void setQueueReadyBatchLoading(
            boolean loading
    ) {
        queueReadyBatchLoading = loading;

        if (loading) {
            setQueueActionButtonsDisabled(true);
        } else {
            updateQueueActionButtons(
                    queueListView.getSelectionModel().getSelectedItem()
            );
        }

        refreshQueueSelectionUi();
    }

    private String formatBatchReadyResult(
            MarkAutoResponseQueueItemsReadyResultDto result
    ) {
        return "Готовыми отмечено: "
                + result.markedReadyCount()
                + ". Уже готовы: "
                + result.alreadyReadyCount()
                + ". Не подходят по статусу: "
                + result.notEligibleCount()
                + ". Не найдены: "
                + result.notFoundCount()
                + ". Ошибок: "
                + result.failedCount()
                + ".";
    }

    private void startReadyAutoResponsesBatch(
            AuthenticatedUserDto currentUser
    ) {
        StartReadyAutoResponsesBatchCommand command =
                new StartReadyAutoResponsesBatchCommand(
                        UserId.of(currentUser.id())
                );

        startReadyAutoResponsesBatchUseCase.start(command)
                .whenComplete((result, throwable) ->
                        Platform.runLater(() -> {
                            autoResponseBatchStartLoading = false;

                            if (throwable != null) {
                                refreshBatchStartButton();

                                batchProgressLabel.setText(
                                        "Не удалось запустить автоотклики: "
                                                + rootMessage(throwable)
                                );

                                return;
                            }

                            handleBatchStartResult(result);
                        })
                );
    }

    private void handleBatchStartResult(
            StartReadyAutoResponsesBatchResultDto result
    ) {
        switch (result.status()) {
            case STARTED -> {
                activateAutoResponseBatch(result.batchId());

                setQueueStatus(
                        "Запущено автооткликов: "
                                + result.plannedCount()
                );

                batchProgressLabel.setText(
                        "Массовый запуск начат. "
                                + "В очереди обработки: "
                                + result.plannedCount()
                );

                startAutoResponseBatchProgressTracking();
            }

            case ALREADY_RUNNING -> {
                activateAutoResponseBatch(result.batchId());

                batchProgressLabel.setText(
                        "Автоотклики уже выполняются. "
                                + "План: "
                                + result.plannedCount()
                );

                startAutoResponseBatchProgressTracking();
            }

            case NO_READY_ITEMS -> {
                activeAutoResponseBatchId = null;
                refreshBatchStartButton();

                batchProgressLabel.setText(
                        "Нет вакансий со статусом «Готова к отклику»."
                );
            }

            case FAILED_TO_START -> {
                activeAutoResponseBatchId = null;
                refreshBatchStartButton();

                batchProgressLabel.setText(
                        "Массовый запуск не начат: "
                                + result.message()
                );
            }
        }
    }

    private void activateAutoResponseBatch(
            UUID batchId
    ) {
        activeAutoResponseBatchId = batchId;

        lastObservedBatchStartedCount = -1;
        lastObservedBatchProcessedCount = -1;

        refreshBatchStartButton();
        refreshQueueSelectionUi();
        updateQueueActionButtons(
                queueListView.getSelectionModel().getSelectedItem()
        );
    }

    private void startAutoResponseBatchProgressTracking() {
        refreshAutoResponseBatchProgress();

        if (autoResponseBatchProgressTimeline != null) {
            autoResponseBatchProgressTimeline.play();
        }
    }

    private void refreshAutoResponseBatchProgress() {
        UUID batchId = activeAutoResponseBatchId;

        if (batchId == null) {
            return;
        }

        Optional<AutoResponseBatchProgressDto> optionalProgress =
                getAutoResponseBatchProgressUseCase.getProgress(batchId);

        if (optionalProgress.isEmpty()) {
            stopAutoResponseBatchProgressTracking(
                    "Данные о массовом запуске больше недоступны."
            );
            return;
        }

        AutoResponseBatchProgressDto progress =
                optionalProgress.get();

        batchProgressLabel.setText(
                formatBatchProgress(progress)
        );

        boolean queueStateChanged =
                progress.startedCount()
                        != lastObservedBatchStartedCount
                        || progress.processedCount()
                        != lastObservedBatchProcessedCount;

        if (queueStateChanged) {
            lastObservedBatchStartedCount =
                    progress.startedCount();

            lastObservedBatchProcessedCount =
                    progress.processedCount();

            loadQueue();
        }

        if (!progress.isActive()) {
            stopAutoResponseBatchProgressTracking(
                    formatBatchProgress(progress)
            );

            loadQueue();
        }
    }

    private void stopAutoResponseBatchProgressTracking(
            String finalMessage
    ) {
        if (autoResponseBatchProgressTimeline != null) {
            autoResponseBatchProgressTimeline.stop();
        }

        activeAutoResponseBatchId = null;
        autoResponseBatchStartLoading = false;

        batchProgressLabel.setText(finalMessage);

        refreshBatchStartButton();
        refreshQueueSelectionUi();

        updateQueueActionButtons(
                queueListView.getSelectionModel().getSelectedItem()
        );
    }

    private boolean isAutoResponseBatchActive() {
        return activeAutoResponseBatchId != null;
    }

    private void refreshBatchStartButton() {
        boolean userAuthenticated = currentUserSession
                .getCurrentUser()
                .isPresent();

        startReadyAutoResponsesBatchButton.setDisable(
                !userAuthenticated
                        || autoResponseBatchStartLoading
                        || isAutoResponseBatchActive()
        );
    }

    private String formatBatchProgress(AutoResponseBatchProgressDto progress) {
        return "Массовый запуск: " + formatBatchProgressStatus(progress.status()) + ". Запущено: " + progress.startedCount() + " из " + progress.plannedCount() + ". Отправлено с подтверждённым письмом: " + progress.sentCount() + ". Частичный результат: " + progress.partialSuccessCount() + ". Ожидают кандидата: " + progress.candidateApprovalRequiredCount() + ". Возвращено в READY: " + progress.returnedToReadyCount() + ". Ошибок: " + progress.failedCount() + ". Пропущено: " + progress.skippedCount() + ".";
    }

    private String formatBatchProgressStatus(
            AutoResponseBatchProgressStatus status
    ) {
        return switch (status) {
            case PREPARING -> "подготовка";
            case RUNNING -> "выполняется";
            case COMPLETED -> "завершён";
            case COMPLETED_WITH_ISSUES ->
                    "завершён с замечаниями";
        };
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
                        clearQueueReadySelection();
                        queueListView.getSelectionModel().clearSelection();
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
        refreshQueueSelectionUi();
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
                        clearQueueReadySelection();
                        queueListView.getSelectionModel().clearSelection();
                        queueListView.getItems().setAll(items);
                        setQueueActionButtonsDisabled(true);
                        refreshQueueSelectionUi();

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

    private void loadSearchPage(
            String text,
            String area,
            int page,
            int perPage
    ) {
        HhVacancySearchQuery query;

        try {
            query = new HhVacancySearchQuery(
                    text,
                    area,
                    page,
                    perPage
            );
        } catch (IllegalArgumentException exception) {
            setStatus(
                    "Некорректные параметры поиска: "
                            + exception.getMessage()
            );
            return;
        }

        searchLoading = true;
        updateSearchControls();

        setStatus(
                "Ищем вакансии на HH.ru: страница "
                        + (page + 1)
                        + "..."
        );

        searchHhVacanciesUseCase.search(query)
                .whenComplete((result, throwable) ->
                        Platform.runLater(() -> {
                            searchLoading = false;

                            if (throwable != null) {
                                setStatus(
                                        "Не удалось выполнить поиск HH.ru: "
                                                + rootMessage(throwable)
                                );

                                updateSearchControls();
                                return;
                            }

                            activeSearchText = text;
                            activeSearchArea = area;
                            currentSearchPage = result.page();
                            currentSearchPerPage = result.perPage() > 0
                                    ? result.perPage()
                                    : perPage;

                            lastSearchResult = result;

                            showSearchResult(result);
                            updateSearchControls();
                        })
                );
    }

    private int selectedPageSize() {
        Integer selectedValue = pageSizeComboBox.getSelectionModel()
                .getSelectedItem();

        return selectedValue == null
                ? DEFAULT_SEARCH_PAGE_SIZE
                : selectedValue;
    }

    private void updateSearchControls() {
        boolean vacancySelected = vacanciesListView.getSelectionModel()
                .getSelectedItem() != null;

        boolean hasSelectedVacancies =
                !getSelectedCurrentPageVacancies().isEmpty();

        boolean interactionLocked = searchLoading
                || queueAdditionLoading;

        boolean hasPreviousPage = lastSearchResult != null
                && lastSearchResult.hasPreviousPage();

        boolean hasNextPage = lastSearchResult != null
                && lastSearchResult.hasNextPage();

        searchButton.setDisable(interactionLocked);

        pageSizeComboBox.setDisable(interactionLocked);

        previousPageButton.setDisable(
                interactionLocked || !hasPreviousPage
        );

        nextPageButton.setDisable(
                interactionLocked || !hasNextPage
        );

        selectAllVacanciesCheckBox.setDisable(
                interactionLocked
                        || vacanciesListView.getItems().isEmpty()
        );

        openVacancyButton.setDisable(
                interactionLocked || !vacancySelected
        );

        addToQueueButton.setDisable(
                interactionLocked || !hasSelectedVacancies
        );

        paginationLabel.setText(
                formatPagination(lastSearchResult)
        );
    }

    private String formatPagination(
            HhVacancySearchResultDto result
    ) {
        if (result == null) {
            return "Страница —";
        }

        if (result.pages() <= 0) {
            return "Страница 0 из 0";
        }

        return "Страница "
                + result.displayedPageNumber()
                + " из "
                + result.pages();
    }

    private void setCurrentPageVacanciesSelected(
            boolean selected
    ) {

        if (queueAdditionLoading) {
            return;
        }

        for (HhVacancyDto vacancy : vacanciesListView.getItems()) {
            updateVacancySelection(vacancy, selected, false);
        }

        refreshVacancySelectionUi();
    }

    private void updateVacancySelection(
            HhVacancyDto vacancy,
            boolean selected,
            boolean refreshUi
    ) {
        if (vacancy == null
                || vacancy.externalId() == null
                || vacancy.externalId().isBlank()) {
            return;
        }

        if (selected) {
            selectedVacancyExternalIds.add(vacancy.externalId());
        } else {
            selectedVacancyExternalIds.remove(vacancy.externalId());
        }

        if (refreshUi) {
            refreshVacancySelectionUi();
        }
    }

    private boolean isVacancySelected(
            HhVacancyDto vacancy
    ) {
        return vacancy != null
                && vacancy.externalId() != null
                && selectedVacancyExternalIds.contains(
                vacancy.externalId()
        );
    }

    private void clearCurrentPageVacancySelection() {
        selectedVacancyExternalIds.clear();

        updatingSelectAllVacancies = true;
        selectAllVacanciesCheckBox.setSelected(false);
        selectAllVacanciesCheckBox.setIndeterminate(false);
        updatingSelectAllVacancies = false;

        selectedVacancyCountLabel.setText("Выбрано: 0");
    }

    private void refreshVacancySelectionUi() {
        int currentPageSize = vacanciesListView.getItems().size();

        int selectedCount = (int) vacanciesListView.getItems()
                .stream()
                .filter(this::isVacancySelected)
                .count();

        boolean allSelected = currentPageSize > 0
                && selectedCount == currentPageSize;

        boolean partiallySelected = selectedCount > 0
                && !allSelected;

        updatingSelectAllVacancies = true;
        selectAllVacanciesCheckBox.setSelected(allSelected);
        selectAllVacanciesCheckBox.setIndeterminate(partiallySelected);
        updatingSelectAllVacancies = false;

        selectedVacancyCountLabel.setText(
                "Выбрано: " + selectedCount
        );

        vacanciesListView.refresh();
    }

    private void showSearchResult(
            HhVacancySearchResultDto result
    ) {
        clearCurrentPageVacancySelection();

        vacanciesListView.getSelectionModel().clearSelection();

        vacanciesListView.getItems().setAll(
                result.vacancies()
        );

        resultCountLabel.setText(
                "Найдено: "
                        + result.found()
                        + " · Показано: "
                        + result.vacancies().size()
        );

        if (result.vacancies().isEmpty()) {
            setStatus("Поиск завершен. Вакансии не найдены.");
            return;
        }

        setStatus(
                "Поиск завершен. Показана страница "
                        + result.displayedPageNumber()
                        + " из "
                        + result.pages()
                        + "."
        );

        refreshVacancySelectionUi();
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

    private String formatQueueItem(
            AutoResponseQueueItemDto item
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append(item.vacancyName())
                .append("\nПлатформа: ")
                .append(item.source())
                .append("\nКомпания: ")
                .append(valueOrUnknown(item.employerName()))
                .append("\nРегион: ")
                .append(valueOrUnknown(item.areaName()))
                .append("\nСтатус: ")
                .append(formatQueueStatus(item.status()))
                .append("\nID HH: ")
                .append(item.externalVacancyId())
                .append("\nСсылка: ")
                .append(valueOrUnknown(item.vacancyUrl()))
                .append("\nДобавлена: ")
                .append(
                        QUEUE_DATE_FORMATTER.format(
                                item.createdAt()
                        )
                );

        if (item.isWaitingCandidateApproval()) {
            builder.append("\nПричина: ")
                    .append(
                            valueOrUnknown(
                                    item.candidateApprovalReason()
                            )
                    )
                    .append("\nDiagnostics: ")
                    .append(
                            valueOrUnknown(
                                    item.diagnosticDirectory()
                            )
                    );
        }

        return builder.toString();
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
        if (QueueActionButtonsAvailabilityPolicy.shouldDisableAll(selectedItem, isAutoResponseBatchActive(), queueReadyBatchLoading)) {
            setQueueActionButtonsDisabled(true);
            return;
        }
        AutoResponseQueueStatus status = selectedItem.status();
        boolean partialSuccess = status == AutoResponseQueueStatus.PARTIAL_SUCCESS;
        removeQueueItemButton.setDisable(false);
        copyQueueVacancyIdButton.setDisable(false);
        openQueueVacancyButton.setDisable(false);
        openQueueDiagnosticsButton.setDisable(!selectedItem.isWaitingCandidateApproval() || selectedItem.diagnosticDirectory() == null || selectedItem.diagnosticDirectory().isBlank());
        executeAutoResponseButton.setDisable(status != AutoResponseQueueStatus.READY);
        markQueueItemReadyButton.setDisable(status == AutoResponseQueueStatus.READY || status == AutoResponseQueueStatus.SENT || partialSuccess);
        returnQueueItemButton.setDisable(status == AutoResponseQueueStatus.QUEUED || status == AutoResponseQueueStatus.SENT || partialSuccess);
        skipQueueItemButton.setDisable(status == AutoResponseQueueStatus.SKIPPED || status == AutoResponseQueueStatus.SENT || partialSuccess);
    }

    private void setQueueActionButtonsDisabled(boolean disabled) {
        executeAutoResponseButton.setDisable(disabled);
        removeQueueItemButton.setDisable(disabled);
        markQueueItemReadyButton.setDisable(disabled);
        returnQueueItemButton.setDisable(disabled);
        skipQueueItemButton.setDisable(disabled);
        copyQueueVacancyIdButton.setDisable(disabled);
        openQueueVacancyButton.setDisable(disabled);
        openQueueDiagnosticsButton.setDisable(disabled);
    }

    private String formatQueueStatus(AutoResponseQueueStatus status) {
        if (status == null) {
            return "не указан";
        }

        return switch (status) {
            case QUEUED -> "В очереди";
            case READY -> "Готова к отклику";
            case IN_PROGRESS -> "Автоотклик выполняется";
            case SENT -> "Отклик отправлен";
            case PARTIAL_SUCCESS -> "Отклик отправлен без подтверждённого письма";
            case FAILED -> "Ошибка отправки";
            case SKIPPED -> "Пропущена";
            case WAITING_CANDIDATE_APPROVAL -> "Ожидается одобрение кандидата";
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

    private void openQueueBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException(
                        "Открытие браузера не поддерживается"
                );
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException(
                        "Открытие браузера не поддерживается"
                );
            }

            desktop.browse(URI.create(url));
        } catch (Exception exception) {
            setQueueStatus(
                    "Не удалось открыть вакансию: "
                            + rootMessage(exception)
            );
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

    private void loadGeneralCoverLetterSettings() {
        AuthenticatedUserDto currentUser = currentUserSession
                .getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setGeneralCoverLetterLoading(true);

            setGeneralCoverLetterStatus(
                    "Для настройки общего письма войдите в аккаунт."
            );

            return;
        }

        setGeneralCoverLetterLoading(true);

        setGeneralCoverLetterStatus(
                "Загружаем сохранённое сопроводительное письмо..."
        );

        getGeneralCoverLetterSettingsUseCase.findByUserId(
                        UserId.of(currentUser.id())
                )
                .whenComplete((optionalSettings, throwable) ->
                        Platform.runLater(() -> {
                            setGeneralCoverLetterLoading(false);

                            if (throwable != null) {
                                setGeneralCoverLetterStatus(
                                        "Не удалось загрузить сохранённое письмо: "
                                                + rootMessage(throwable)
                                );
                                return;
                            }

                            if (optionalSettings.isEmpty()) {
                                replaceGeneralCoverLetterContent("");

                                generalCoverLetterSourceFileName = null;

                                useGeneralCoverLetterWhenLlmUnavailableCheckBox
                                        .setSelected(false);

                                setGeneralCoverLetterStatus(
                                        "Общее сопроводительное письмо ещё не сохранено."
                                );
                                return;
                            }

                            GeneralCoverLetterSettingsDto settings =
                                    optionalSettings.get();

                            replaceGeneralCoverLetterContent(
                                    settings.content()
                            );

                            generalCoverLetterSourceFileName =
                                    settings.sourceFileName();

                            useGeneralCoverLetterWhenLlmUnavailableCheckBox
                                    .setSelected(
                                            settings.useWhenLlmUnavailable()
                                    );

                            String sourceInfo =
                                    settings.sourceFileName() == null
                                            ? "введено вручную"
                                            : "загружено из "
                                            + settings.sourceFileName();

                            setGeneralCoverLetterStatus(
                                    "Сохранено: "
                                            + sourceInfo
                                            + ". Fallback: "
                                            + (settings.useWhenLlmUnavailable()
                                            ? "включён"
                                            : "выключен")
                                            + "."
                            );
                        })
                );
    }

    private void replaceGeneralCoverLetterContent(String content) {
        replacingGeneralCoverLetterContent = true;

        try {
            generalCoverLetterTextArea.setText(content);
        } finally {
            replacingGeneralCoverLetterContent = false;
        }
    }

    private void setGeneralCoverLetterLoading(boolean loading) {
        generalCoverLetterLoading = loading;

        generalCoverLetterTextArea.setDisable(loading);

        useGeneralCoverLetterWhenLlmUnavailableCheckBox.setDisable(
                loading
        );

        loadGeneralCoverLetterTextButton.setDisable(loading);

        saveGeneralCoverLetterButton.setDisable(loading);
    }

    private void setGeneralCoverLetterStatus(String message) {
        generalCoverLetterStatusLabel.setText(message);
    }

    private void loadPrimaryResumeStatus() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            resumeStatusLabel.setText("Для загрузки резюме войдите в аккаунт.");
            resumeUploadPdfButton.setDisable(true);
            return;
        }

        resumeUploadPdfButton.setDisable(true);
        resumeStatusLabel.setText("Проверяем основное резюме...");

        getPrimaryResumeUseCase.getPrimaryResume(UserId.of(currentUser.id()))
                .whenComplete((optionalResume, throwable) -> Platform.runLater(() -> {
                    resumeUploadPdfButton.setDisable(false);

                    if (throwable != null) {
                        resumeStatusLabel.setText(
                                "Не удалось проверить основное резюме: "
                                        + rootMessage(throwable)
                        );
                        return;
                    }

                    if (optionalResume.isEmpty()) {
                        resumeStatusLabel.setText(
                                "Основное резюме не загружено. "
                                        + "Загрузите PDF перед запуском автоотклика."
                        );
                        resumeUploadPdfButton.setText("Загрузить PDF");
                        return;
                    }

                    ResumeDto resume = optionalResume.get();

                    resumeStatusLabel.setText(formatPrimaryResumeStatus(resume));
                    resumeUploadPdfButton.setText("Заменить PDF");
                }));
    }

    private String formatPrimaryResumeStatus(ResumeDto resume) {
        String fileName = resume.originalFileName();

        if (fileName == null || fileName.isBlank()) {
            return "Основное резюме: " + resume.title();
        }

        return "Основное резюме: "
                + resume.title()
                + " ("
                + fileName
                + ")";
    }

    private void executeAutoResponse(
            AuthenticatedUserDto currentUser,
            AutoResponseQueueItemDto selectedItem
    ) {
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
                        updateQueueActionButtons(
                                queueListView.getSelectionModel().getSelectedItem()
                        );

                        setQueueStatus(
                                "Не удалось выполнить автоотклик: "
                                        + rootMessage(throwable)
                        );
                    }
                }));
    }

    private final class VacancySelectionListCell
            extends ListCell<HhVacancyDto> {

        private final CheckBox selectionCheckBox = new CheckBox();

        private final Label vacancyLabel = new Label();

        private final HBox content = new HBox(
                10,
                selectionCheckBox,
                vacancyLabel
        );

        private VacancySelectionListCell() {
            vacancyLabel.setWrapText(true);
            vacancyLabel.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(
                    vacancyLabel,
                    Priority.ALWAYS
            );

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            selectionCheckBox.setOnAction(event -> {
                HhVacancyDto vacancy = getItem();

                if (vacancy != null) {
                    updateVacancySelection(
                            vacancy,
                            selectionCheckBox.isSelected(),
                            true
                    );
                }
            });
        }

        @Override
        protected void updateItem(
                HhVacancyDto vacancy,
                boolean empty
        ) {
            super.updateItem(vacancy, empty);

            if (empty || vacancy == null) {
                setGraphic(null);
                return;
            }

            selectionCheckBox.setSelected(
                    isVacancySelected(vacancy)
            );

            selectionCheckBox.setDisable(
                    searchLoading || queueAdditionLoading
            );

            vacancyLabel.setText(
                    formatVacancy(vacancy)
            );

            setGraphic(content);
        }
    }

    private final class QueueSelectionListCell
            extends ListCell<AutoResponseQueueItemDto> {

        private final CheckBox selectionCheckBox = new CheckBox();

        private final Label queueItemLabel = new Label();

        private final HBox content = new HBox(
                10,
                selectionCheckBox,
                queueItemLabel
        );

        private QueueSelectionListCell() {
            queueItemLabel.setWrapText(true);
            queueItemLabel.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(
                    queueItemLabel,
                    Priority.ALWAYS
            );

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            selectionCheckBox.setOnAction(event -> {
                AutoResponseQueueItemDto item = getItem();

                if (item != null) {
                    updateQueueItemReadySelection(
                            item,
                            selectionCheckBox.isSelected(),
                            true
                    );
                }
            });
        }

        @Override
        protected void updateItem(
                AutoResponseQueueItemDto item,
                boolean empty
        ) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            boolean eligibleForReady = item.status()
                    == AutoResponseQueueStatus.QUEUED;

            selectionCheckBox.setSelected(
                    isQueueItemSelectedForReady(item)
            );

            selectionCheckBox.setDisable(
                    !eligibleForReady
                            || queueReadyBatchLoading
                            || isAutoResponseBatchActive()
            );

            queueItemLabel.setText(
                    formatQueueItem(item)
            );

            setGraphic(content);
        }
    }
}