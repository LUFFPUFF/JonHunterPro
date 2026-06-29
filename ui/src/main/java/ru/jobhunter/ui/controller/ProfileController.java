package ru.jobhunter.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.*;
import ru.jobhunter.core.application.usecase.integration.*;
import ru.jobhunter.core.application.usecase.profile.GetCandidateQuestionnaireProfileUseCase;
import ru.jobhunter.core.application.usecase.profile.SaveCandidateQuestionnaireProfileUseCase;
import ru.jobhunter.core.domain.model.*;
import ru.jobhunter.ui.navigation.UiNavigator;
import ru.jobhunter.ui.session.CurrentUserSession;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.awt.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private static final DateTimeFormatter TOKEN_EXPIRATION_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final CurrentUserSession currentUserSession;
    private final UiNavigator uiNavigator;
    private final ConnectHhAccountUseCase connectHhAccountUseCase;
    private final ConnectHabrCareerAccountUseCase connectHabrCareerAccountUseCase;
    private final GetHhConnectionStatusUseCase getHhConnectionStatusUseCase;
    private final GetHhCurrentUserUseCase getHhCurrentUserUseCase;
    private final GetHhResumesUseCase getHhResumesUseCase;
    private final GetHabrCareerCurrentUserUseCase getHabrCareerCurrentUserUseCase;
    private final GetCandidateQuestionnaireProfileUseCase getCandidateQuestionnaireProfileUseCase;
    private final SaveCandidateQuestionnaireProfileUseCase saveCandidateQuestionnaireProfileUseCase;

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

    @FXML
    private Button checkHhResumesButton;

    @FXML
    private Label hhResumesStatusLabel;

    @FXML
    private Button connectHabrCareerButton;

    @FXML
    private Label habrCareerConnectionStatusLabel;

    @FXML
    private Button checkHabrCareerApiButton;

    @FXML
    private Label habrCareerApiStatusLabel;

    @FXML
    private TextField candidateTimeZoneIdField;

    @FXML
    private TextField candidateSalaryMinField;

    @FXML
    private TextField candidateSalaryMaxField;

    @FXML
    private TextField candidateSalaryCurrencyField;

    @FXML
    private ComboBox<CandidateSalaryTaxBasis> candidateSalaryTaxBasisComboBox;

    @FXML
    private CheckBox candidateRelocationReadyCheckBox;

    @FXML
    private ComboBox<CandidateWorkFormatPreference>
            candidateWorkFormatPreferenceComboBox;

    @FXML
    private CheckBox candidateRemoteWorkPriorityCheckBox;

    @FXML
    private TextField candidateEnglishLevelField;

    @FXML
    private CheckBox candidateBusinessTripsReadyCheckBox;

    @FXML
    private ComboBox<CandidateTestAssignmentReadiness> candidateTestAssignmentReadinessComboBox;

    @FXML
    private ComboBox<CandidateStartAvailability>
            candidateStartAvailabilityComboBox;

    @FXML
    private CheckBox candidateAllowRelatedExperienceDraftsCheckBox;

    @FXML
    private TextArea candidateAdditionalConfirmedFactsTextArea;

    @FXML
    private Button saveCandidateQuestionnaireProfileButton;

    @FXML
    private Label candidateQuestionnaireProfileStatusLabel;

    public ProfileController(
            CurrentUserSession currentUserSession,
            UiNavigator uiNavigator,
            ConnectHhAccountUseCase connectHhAccountUseCase,
            GetHhConnectionStatusUseCase getHhConnectionStatusUseCase,
            GetHhCurrentUserUseCase getHhCurrentUserUseCase,
            GetHhResumesUseCase getHhResumesUseCase,
            ConnectHabrCareerAccountUseCase connectHabrCareerAccountUseCase,
            GetHabrCareerCurrentUserUseCase getHabrCareerCurrentUserUseCase,
            GetCandidateQuestionnaireProfileUseCase getCandidateQuestionnaireProfileUseCase,
            SaveCandidateQuestionnaireProfileUseCase saveCandidateQuestionnaireProfileUseCase
    ){
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
        this.getHhResumesUseCase = Objects.requireNonNull(
                getHhResumesUseCase,
                "Get HH resumes use case must not be null"
        );
        this.getHabrCareerCurrentUserUseCase = Objects.requireNonNull(
                getHabrCareerCurrentUserUseCase,
                "Get Habr Career current user use case must not be null"
        );
        this.connectHabrCareerAccountUseCase = Objects.requireNonNull(
                connectHabrCareerAccountUseCase,
                "Connect Habr Career account use case must not be null"
        );
        this.getCandidateQuestionnaireProfileUseCase = Objects.requireNonNull(
                getCandidateQuestionnaireProfileUseCase,
                "Get candidate questionnaire profile use case must not be null"
        );
        this.saveCandidateQuestionnaireProfileUseCase = Objects.requireNonNull(
                saveCandidateQuestionnaireProfileUseCase,
                "Save candidate questionnaire profile use case must not be null"
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
        setHhResumesStatus("Резюме HH.ru не проверялись");
        setHabrCareerStatus("Habr Career не подключён");
        setHabrCareerApiStatus("Habr Career API не проверялся");
        configureCandidateQuestionnaireProfileControls();
        applySuggestedCandidateQuestionnaireProfileDefaults();
        setCandidateQuestionnaireProfileStatus(
                "Факты кандидата ещё не сохранены."
        );

        UserId userId = UserId.of(user.id());
        loadHhConnectionStatus(userId);
        loadCandidateQuestionnaireProfile(userId);

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
    private void onCheckHhResumesClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setHhResumesStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        UserId userId = UserId.of(currentUser.id());
        checkHhResumes(userId);
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
    private void onConnectHabrCareerClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setHabrCareerStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        connectHabrCareerButton.setDisable(true);
        setHabrCareerStatus("Открываем страницу авторизации Habr Career...");

        try {
            UserId userId = UserId.of(currentUser.id());

            HabrCareerConnectionFlowDto flow =
                    connectHabrCareerAccountUseCase.startConnection(userId);

            openBrowser(flow.authorizationUrl());

            setHabrCareerStatus(
                    "Авторизуйтесь в браузере. После разрешения доступа вернитесь в приложение."
            );

            flow.completion().whenComplete((result, throwable) -> Platform.runLater(() -> {
                connectHabrCareerButton.setDisable(false);

                if (throwable == null) {
                    setHabrCareerStatus("Habr Career подключён.");
                    checkHabrCareerApi(userId);
                } else {
                    setHabrCareerStatus(
                            "Не удалось подключить Habr Career: " + rootMessage(throwable)
                    );
                }
            }));
        } catch (RuntimeException exception) {
            connectHabrCareerButton.setDisable(false);
            setHabrCareerStatus(
                    "Не удалось начать подключение Habr Career: " + rootMessage(exception)
            );
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

    @FXML
    private void onCheckHabrCareerApiClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setHabrCareerApiStatus("Сначала войдите в аккаунт JobHunterPro.");
            return;
        }

        UserId userId = UserId.of(currentUser.id());
        checkHabrCareerApi(userId);
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

    private void setHabrCareerStatus(String message) {
        if (habrCareerConnectionStatusLabel != null) {
            habrCareerConnectionStatusLabel.setText(message);
        }
    }

    private String formatHabrCareerApiStatus(HabrCareerCurrentUserDto habrCareerUser) {
        String login = valueOrUnknown(habrCareerUser.login());
        String email = valueOrUnknown(habrCareerUser.email());
        String city = valueOrUnknown(habrCareerUser.city());

        return "Habr Career API доступен. Логин: " + login + ", email: " + email + ", город: " + city;
    }

    private void checkHabrCareerApi(UserId userId) {
        checkHabrCareerApiButton.setDisable(true);
        setHabrCareerApiStatus("Проверяем доступ к Habr Career API...");

        getHabrCareerCurrentUserUseCase.getCurrentUser(userId)
                .whenComplete((habrCareerUser, throwable) -> Platform.runLater(() -> {
                    checkHabrCareerApiButton.setDisable(false);

                    if (throwable == null) {
                        setHabrCareerApiStatus(formatHabrCareerApiStatus(habrCareerUser));
                    } else {
                        setHabrCareerApiStatus("Habr Career API недоступен: " + rootMessage(throwable));
                    }
                }));
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

    private void checkHhResumes(UserId userId) {
        checkHhResumesButton.setDisable(true);
        setHhResumesStatus("Загружаем резюме HH.ru...");

        getHhResumesUseCase.getResumes(userId)
                .whenComplete((resumes, throwable) -> Platform.runLater(() -> {
                    checkHhResumesButton.setDisable(false);

                    if (throwable == null) {
                        setHhResumesStatus(formatHhResumesStatus(resumes));
                    } else {
                        setHhResumesStatus("Не удалось загрузить резюме HH.ru: " + rootMessage(throwable));
                    }
                }));
    }

    private String formatHhResumesStatus(List<HhResumeDto> resumes) {
        if (resumes == null || resumes.isEmpty()) {
            return "HH.ru резюме не найдены.";
        }

        StringBuilder builder = new StringBuilder("Найдено резюме: ")
                .append(resumes.size());

        for (HhResumeDto resume : resumes.stream().limit(5).toList()) {
            builder.append("\n")
                    .append("ID: ")
                    .append(resume.id())
                    .append(" — ")
                    .append(valueOrUnknown(resume.title()));

            if (resume.statusName() != null) {
                builder.append(" [").append(resume.statusName()).append("]");
            }
        }

        return builder.toString();
    }

    private void setHhResumesStatus(String message) {
        if (hhResumesStatusLabel != null) {
            hhResumesStatusLabel.setText(message);
        }
    }

    private void setHhStatus(String message) {
        if (hhConnectionStatusLabel != null) {
            hhConnectionStatusLabel.setText(message);
        }
    }

    private void setHabrCareerApiStatus(String message) {
        if (habrCareerApiStatusLabel != null) {
            habrCareerApiStatusLabel.setText(message);
        }
    }

    private void setHhApiStatus(String message) {
        if (hhApiStatusLabel != null) {
            hhApiStatusLabel.setText(message);
        }
    }

    @FXML
    private void onSaveCandidateQuestionnaireProfileClicked() {
        AuthenticatedUserDto currentUser = currentUserSession.getCurrentUser()
                .orElse(null);

        if (currentUser == null) {
            setCandidateQuestionnaireProfileStatus(
                    "Сначала войдите в аккаунт JobHunterPro."
            );
            return;
        }

        try {
            SaveCandidateQuestionnaireProfileCommand command =
                    new SaveCandidateQuestionnaireProfileCommand(
                            UserId.of(currentUser.id()),
                            candidateTimeZoneIdField.getText(),
                            parseMoney(
                                    candidateSalaryMinField.getText(),
                                    "Минимальная зарплата"
                            ),
                            parseMoney(
                                    candidateSalaryMaxField.getText(),
                                    "Максимальная зарплата"
                            ),
                            candidateSalaryCurrencyField.getText(),
                            requireSelected(
                                    candidateSalaryTaxBasisComboBox.getValue(),
                                    "Укажите основу суммы зарплаты"
                            ),
                            candidateRelocationReadyCheckBox.isSelected(),
                            requireSelected(
                                    candidateWorkFormatPreferenceComboBox.getValue(),
                                    "Укажите предпочтительный формат работы"
                            ),
                            candidateRemoteWorkPriorityCheckBox.isSelected(),
                            candidateEnglishLevelField.getText(),
                            candidateBusinessTripsReadyCheckBox.isSelected(),
                            requireSelected(
                                    candidateTestAssignmentReadinessComboBox.getValue(),
                                    "Укажите готовность к тестовому заданию"
                            ),
                            requireSelected(
                                    candidateStartAvailabilityComboBox.getValue(),
                                    "Укажите готовность начать работу"
                            ),
                            candidateAllowRelatedExperienceDraftsCheckBox.isSelected(),
                            candidateAdditionalConfirmedFactsTextArea.getText()
                    );

            setCandidateQuestionnaireProfileSaveInProgress(true);
            setCandidateQuestionnaireProfileStatus("Сохраняем факты кандидата...");

            saveCandidateQuestionnaireProfileUseCase.save(command)
                    .whenComplete((profile, throwable) -> Platform.runLater(() -> {
                        setCandidateQuestionnaireProfileSaveInProgress(false);

                        if (throwable == null) {
                            applyCandidateQuestionnaireProfile(profile);
                            setCandidateQuestionnaireProfileStatus(
                                    "Факты кандидата сохранены. "
                                            + "Они будут использоваться для ответов на анкеты HH.ru."
                            );
                            return;
                        }

                        setCandidateQuestionnaireProfileStatus(
                                "Не удалось сохранить факты кандидата: "
                                        + rootMessage(throwable)
                        );
                    }));
        } catch (RuntimeException exception) {
            setCandidateQuestionnaireProfileStatus(
                    "Проверьте данные профиля: " + rootMessage(exception)
            );
        }
    }

    private void loadCandidateQuestionnaireProfile(UserId userId) {
        setCandidateQuestionnaireProfileSaveInProgress(true);
        setCandidateQuestionnaireProfileStatus("Загружаем факты кандидата...");

        getCandidateQuestionnaireProfileUseCase.findByUserId(userId)
                .whenComplete((optionalProfile, throwable) -> Platform.runLater(() -> {
                    setCandidateQuestionnaireProfileSaveInProgress(false);

                    if (throwable != null) {
                        setCandidateQuestionnaireProfileStatus(
                                "Не удалось загрузить факты кандидата: "
                                        + rootMessage(throwable)
                        );
                        return;
                    }

                    optionalProfile.ifPresentOrElse(
                            profile -> {
                                applyCandidateQuestionnaireProfile(profile);
                                setCandidateQuestionnaireProfileStatus(
                                        "Факты кандидата загружены."
                                );
                            },
                            () -> setCandidateQuestionnaireProfileStatus(
                                    "Профиль фактов ещё не сохранён. "
                                            + "Проверьте предложенные значения и сохраните их."
                            )
                    );
                }));
    }

    private void configureCandidateQuestionnaireProfileControls() {
        candidateSalaryTaxBasisComboBox.getItems().setAll(
                CandidateSalaryTaxBasis.values()
        );
        candidateWorkFormatPreferenceComboBox.getItems().setAll(
                CandidateWorkFormatPreference.values()
        );
        candidateStartAvailabilityComboBox.getItems().setAll(
                CandidateStartAvailability.values()
        );
        candidateTestAssignmentReadinessComboBox.getItems().setAll(
                CandidateTestAssignmentReadiness.values()
        );

        candidateSalaryTaxBasisComboBox.setConverter(
                displayConverter(this::formatSalaryTaxBasis)
        );
        candidateWorkFormatPreferenceComboBox.setConverter(
                displayConverter(this::formatWorkFormatPreference)
        );
        candidateStartAvailabilityComboBox.setConverter(
                displayConverter(this::formatStartAvailability)
        );
        candidateTestAssignmentReadinessComboBox.setConverter(
                displayConverter(this::formatTestAssignmentReadiness)
        );
    }

    private void applySuggestedCandidateQuestionnaireProfileDefaults() {
        candidateTimeZoneIdField.setText("Europe/Moscow");
        candidateSalaryMinField.setText("90000");
        candidateSalaryMaxField.setText("150000");
        candidateSalaryCurrencyField.setText("RUB");
        candidateSalaryTaxBasisComboBox.setValue(
                CandidateSalaryTaxBasis.UNSPECIFIED
        );
        candidateRelocationReadyCheckBox.setSelected(false);
        candidateWorkFormatPreferenceComboBox.setValue(
                CandidateWorkFormatPreference.ANY
        );
        candidateRemoteWorkPriorityCheckBox.setSelected(true);
        candidateEnglishLevelField.setText("B2");
        candidateBusinessTripsReadyCheckBox.setSelected(true);
        candidateTestAssignmentReadinessComboBox.setValue(
                CandidateTestAssignmentReadiness.UNKNOWN
        );
        candidateStartAvailabilityComboBox.setValue(
                CandidateStartAvailability.IMMEDIATELY
        );
        candidateAllowRelatedExperienceDraftsCheckBox.setSelected(true);
        candidateAdditionalConfirmedFactsTextArea.clear();
    }

    private void applyCandidateQuestionnaireProfile(
            CandidateQuestionnaireProfileDto profile
    ) {
        candidateTimeZoneIdField.setText(profile.timeZoneId());
        candidateSalaryMinField.setText(formatAmount(profile.salaryMin()));
        candidateSalaryMaxField.setText(formatAmount(profile.salaryMax()));
        candidateSalaryCurrencyField.setText(profile.salaryCurrency());
        candidateSalaryTaxBasisComboBox.setValue(profile.salaryTaxBasis());
        candidateRelocationReadyCheckBox.setSelected(profile.relocationReady());
        candidateWorkFormatPreferenceComboBox.setValue(
                profile.workFormatPreference()
        );
        candidateRemoteWorkPriorityCheckBox.setSelected(
                profile.remoteWorkPriority()
        );
        candidateEnglishLevelField.setText(profile.englishLevel());
        candidateBusinessTripsReadyCheckBox.setSelected(
                profile.businessTripsReady()
        );
        candidateTestAssignmentReadinessComboBox.setValue(
                profile.testAssignmentReadiness()
        );
        candidateStartAvailabilityComboBox.setValue(
                profile.startAvailability()
        );
        candidateAllowRelatedExperienceDraftsCheckBox.setSelected(
                profile.allowRelatedExperienceDrafts()
        );
        candidateAdditionalConfirmedFactsTextArea.setText(
                profile.additionalConfirmedFacts()
        );
    }

    private void setCandidateQuestionnaireProfileSaveInProgress(
            boolean inProgress
    ) {
        if (saveCandidateQuestionnaireProfileButton != null) {
            saveCandidateQuestionnaireProfileButton.setDisable(inProgress);
        }
    }

    private void setCandidateQuestionnaireProfileStatus(String message) {
        if (candidateQuestionnaireProfileStatusLabel != null) {
            candidateQuestionnaireProfileStatusLabel.setText(message);
        }
    }

    private String formatTestAssignmentReadiness(
            CandidateTestAssignmentReadiness readiness
    ) {
        return switch (readiness) {
            case YES -> "Готов выполнить";
            case NO -> "Не рассматриваю";
            case UNKNOWN -> "Не указано";
        };
    }

    private BigDecimal parseMoney(String rawValue, String fieldName) {
        String normalizedValue = rawValue == null
                ? ""
                : rawValue
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(",", ".")
                .trim();

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " не может быть пустой"
            );
        }

        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    fieldName + " должна быть числом",
                    exception
            );
        }
    }

    private <T> T requireSelected(T value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value;
    }

    private String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String formatSalaryTaxBasis(CandidateSalaryTaxBasis taxBasis) {
        return switch (taxBasis) {
            case UNSPECIFIED -> "Не указано";
            case GROSS -> "До вычета налогов";
            case NET -> "На руки";
        };
    }

    private String formatWorkFormatPreference(
            CandidateWorkFormatPreference workFormatPreference
    ) {
        return switch (workFormatPreference) {
            case ANY -> "Любой";
            case REMOTE -> "Удалённый";
            case HYBRID -> "Гибридный";
            case OFFICE -> "Офис";
        };
    }

    private String formatStartAvailability(
            CandidateStartAvailability startAvailability
    ) {
        return switch (startAvailability) {
            case IMMEDIATELY -> "Сразу";
            case WITHIN_TWO_WEEKS -> "В течение двух недель";
            case WITHIN_ONE_MONTH -> "В течение месяца";
            case NEGOTIABLE -> "По договорённости";
        };
    }

    private <T> StringConverter<T> displayConverter(
            Function<T, String> formatter
    ) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : formatter.apply(value);
            }

            @Override
            public T fromString(String value) {
                return null;
            }
        };
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