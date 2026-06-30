package ru.jobhunter.infrastructure.platform.habr.browser;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.HabrCareerResponseFormControlDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reads Habr Career response-action metadata with narrow DOM selectors.
 *
 * <p>This extractor has no method that writes candidate data or invokes save,
 * submit, or delete. The sole browser mutation approved for the dedicated
 * edit-form probe is performed by that probe service after it resolves the
 * exact visible "Редактировать" button exposed here.</p>
 */
@Component
public final class HabrCareerResponseFormExtractor {

    static final String RESPONSE_CONTAINER_SELECTOR =
            "#create-vacancy-response";
    static final String INITIAL_RESPONSE_ACTION_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR + " button[type='submit']";
    static final String SSR_STATE_SELECTOR =
            "script[data-ssr-state='true']";
    static final String FORM_CONTROL_SELECTOR =
            "textarea, input:not([type='hidden']), select, "
                    + "[contenteditable='true']";
    static final String VISIBLE_BUTTON_SELECTOR = "button";
    static final String TERMINAL_RESPONSE_RESULT_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR
                    + " .create-vacancy-response__action-result"
                    + ".action-result-box--appearance-success";
    static final String TERMINAL_RESPONSE_TITLE_SELECTOR =
            TERMINAL_RESPONSE_RESULT_SELECTOR + " .action-result-box__title";
    static final String POST_RESPONSE_FORM_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR + " form.basic-form";
    static final String POST_RESPONSE_COVER_LETTER_SELECTOR =
            POST_RESPONSE_FORM_SELECTOR + " textarea[name='body']";
    static final String POST_RESPONSE_SALARY_SELECTOR =
            POST_RESPONSE_FORM_SELECTOR + " input[name='user[salary]']";
    static final String POST_RESPONSE_CURRENCY_SELECTOR =
            POST_RESPONSE_FORM_SELECTOR + " select[name='user[currency]']";
    static final String POST_RESPONSE_COMPLEMENT_SUBMIT_SELECTOR =
            POST_RESPONSE_FORM_SELECTOR + " button[type='submit']";
    static final String EXISTING_RESPONSE_CARD_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR
                    + " form.basic-form .vacancy-response article.resume-card";
    static final String EXISTING_RESPONSE_EDIT_ACTION_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR
                    + " .create-vacancy-response__controls "
                    + "button.create-vacancy-response__button";
    static final String RESPONSE_EDIT_COVER_LETTER_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR
                    + " .vacancy-response textarea[name='body']";
    static final String RESPONSE_EDIT_SAVE_ACTION_SELECTOR =
            RESPONSE_CONTAINER_SELECTOR
                    + " .vacancy-response button[type='submit']";

    private static final String INITIAL_RESPONSE_ACTION_TEXT =
            "Откликнуться";
    private static final String COMPLEMENT_RESPONSE_ACTION_TEXT =
            "Дополнить отклик";
    private static final String EXISTING_RESPONSE_EDIT_ACTION_TEXT =
            "Редактировать";
    private static final String RESPONSE_EDIT_SAVE_ACTION_TEXT = "Сохранить";
    private static final String DIRECT_RESPONSE_KIND = "direct";

    private static final List<String> TERMINAL_RESPONSE_MARKERS = List.of(
            "отклик отправлен",
            "ваш отклик отправлен",
            "вы уже откликнулись",
            "отклик уже отправлен"
    );

    public boolean hasInitialResponseAction(WebDriver driver) {
        return findInitialResponseAction(driver).isPresent();
    }

    /**
     * Returns {@code true} only when the server-rendered vacancy state marks
     * the action as {@code response.kind = direct}. On Habr Career this means
     * that clicking the initial action can create a real response immediately.
     */
    public boolean isDirectResponseAction(WebDriver driver) {
        return DIRECT_RESPONSE_KIND.equalsIgnoreCase(
                responseActionKind(driver)
        );
    }

    /**
     * Reads {@code vacancy.response.kind} from the exact SSR state script.
     * No broad DOM scan and no browser mutation are performed.
     */
    public String responseActionKind(WebDriver driver) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        Object value = ((JavascriptExecutor) driver).executeScript(
                "const stateElement = document.querySelector(arguments[0]);"
                        + "if (!stateElement) { return ''; }"
                        + "try {"
                        + "  const state = JSON.parse(stateElement.textContent || '');"
                        + "  return state && state.vacancy && state.vacancy.response"
                        + "      ? state.vacancy.response.kind || ''"
                        + "      : '';"
                        + "} catch (error) {"
                        + "  return '';"
                        + "}",
                SSR_STATE_SELECTOR
        );

        return normalize(value == null ? "" : String.valueOf(value));
    }

    public boolean hasVisibleResponseFormControls(WebDriver driver) {
        return !extractControls(driver).isEmpty();
    }

    public boolean hasExistingEditableResponse(WebDriver driver) {
        return hasVisibleElement(driver, EXISTING_RESPONSE_CARD_SELECTOR)
                && findExistingResponseEditAction(driver).isPresent();
    }

    /**
     * Finds only the exact visible edit action of an already existing response.
     * The caller decides whether the user-approved diagnostic probe may click
     * it. The extractor itself never mutates the page.
     */
    public Optional<WebElement> findExistingResponseEditAction(
            WebDriver driver
    ) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return driver.findElements(
                        By.cssSelector(EXISTING_RESPONSE_EDIT_ACTION_SELECTOR)
                )
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .filter(element -> isExistingResponseEditActionText(
                        element.getText()
                ))
                .findFirst();
    }

    /**
     * Finds the cover-letter textarea in the edit form of an existing response.
     * The method only reads the DOM and never mutates the field.
     */
    public Optional<WebElement> findExistingResponseCoverLetterInput(
            WebDriver driver
    ) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return driver.findElements(
                        By.cssSelector(RESPONSE_EDIT_COVER_LETTER_SELECTOR)
                )
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .findFirst();
    }

    /**
     * Finds the exact visible save action of an existing response edit form.
     * The caller must explicitly decide whether this real mutation is allowed.
     */
    public Optional<WebElement> findExistingResponseEditSaveAction(
            WebDriver driver
    ) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return driver.findElements(
                        By.cssSelector(RESPONSE_EDIT_SAVE_ACTION_SELECTOR)
                )
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .filter(element -> isExistingResponseEditSaveActionText(
                        element.getText()
                ))
                .findFirst();
    }

    /**
     * Reads a textarea value through the DOM property. {@link WebElement#getText()}
     * is not reliable for a textarea after user input.
     */
    public String readInputValue(WebDriver driver, WebElement input) {
        Objects.requireNonNull(driver, "Web driver must not be null");
        Objects.requireNonNull(input, "Input element must not be null");

        Object value = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].value || '';",
                input
        );

        return normalize(value == null ? "" : String.valueOf(value));
    }

    /**
     * Returns the browser-advertised maxlength or {@code 0} when no limit is
     * declared by the form.
     */
    public int inputMaxLength(WebElement input) {
        Objects.requireNonNull(input, "Input element must not be null");

        String value = normalize(input.getAttribute("maxlength"));

        if (value.isBlank()) {
            return 0;
        }

        try {
            int maxLength = Integer.parseInt(value);
            return Math.max(0, maxLength);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public boolean hasTerminalResponseState(WebDriver driver) {
        return !terminalResponseMarker(driver).isBlank();
    }

    /**
     * Reads the exact success title rendered by Habr Career after a direct
     * response was created. The method does not mutate the page.
     */
    public String terminalResponseMarker(WebDriver driver) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return driver.findElements(By.cssSelector(TERMINAL_RESPONSE_TITLE_SELECTOR))
                .stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .map(HabrCareerResponseFormExtractor::normalize)
                .filter(HabrCareerResponseFormExtractor::containsTerminalResponseMarker)
                .findFirst()
                .orElse("");
    }

    /**
     * Returns {@code true} only for the already-sent-response form that has a
     * cover-letter input and the dedicated "Дополнить отклик" submit action.
     */
    public boolean hasPostResponseForm(WebDriver driver) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return !terminalResponseMarker(driver).isBlank()
                && hasVisibleElement(driver, POST_RESPONSE_COVER_LETTER_SELECTOR)
                && hasComplementResponseAction(driver);
    }

    public boolean hasComplementResponseAction(WebDriver driver) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return driver.findElements(
                        By.cssSelector(POST_RESPONSE_COMPLEMENT_SUBMIT_SELECTOR)
                )
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .map(WebElement::getText)
                .anyMatch(HabrCareerResponseFormExtractor
                        ::isComplementResponseActionText);
    }

    public List<HabrCareerResponseFormControlDto> extractControls(
            WebDriver driver
    ) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        Optional<WebElement> container = findResponseContainer(driver);

        if (container.isEmpty()) {
            return List.of();
        }

        List<HabrCareerResponseFormControlDto> controls = new ArrayList<>();

        for (WebElement element : container.get().findElements(
                By.cssSelector(FORM_CONTROL_SELECTOR)
        )) {
            if (!element.isDisplayed()) {
                continue;
            }

            controls.add(new HabrCareerResponseFormControlDto(
                    normalize(element.getTagName()),
                    normalize(element.getAttribute("type")),
                    normalize(element.getAttribute("name")),
                    normalize(element.getAttribute("id")),
                    normalize(element.getAttribute("placeholder")),
                    resolveLabel(driver, element),
                    Boolean.parseBoolean(element.getAttribute("required")),
                    !element.isEnabled()
            ));
        }

        return List.copyOf(controls);
    }

    public List<String> extractVisibleButtonLabels(WebDriver driver) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        Optional<WebElement> container = findResponseContainer(driver);

        if (container.isEmpty()) {
            return List.of();
        }

        Set<String> labels = new LinkedHashSet<>();

        for (WebElement button : container.get().findElements(
                By.cssSelector(VISIBLE_BUTTON_SELECTOR)
        )) {
            if (!button.isDisplayed()) {
                continue;
            }

            String label = normalize(button.getText());

            if (!label.isBlank()) {
                labels.add(label);
            }
        }

        return List.copyOf(labels);
    }

    static boolean isInitialResponseActionText(String value) {
        return INITIAL_RESPONSE_ACTION_TEXT.equalsIgnoreCase(normalize(value));
    }

    static boolean isDirectResponseKind(String value) {
        return DIRECT_RESPONSE_KIND.equalsIgnoreCase(normalize(value));
    }

    static boolean isComplementResponseActionText(String value) {
        return COMPLEMENT_RESPONSE_ACTION_TEXT.equalsIgnoreCase(normalize(value));
    }

    static boolean isExistingResponseEditActionText(String value) {
        return EXISTING_RESPONSE_EDIT_ACTION_TEXT.equalsIgnoreCase(
                normalize(value)
        );
    }

    static boolean isExistingResponseEditSaveActionText(String value) {
        return RESPONSE_EDIT_SAVE_ACTION_TEXT.equalsIgnoreCase(
                normalize(value)
        );
    }

    static boolean containsTerminalResponseMarker(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ROOT);

        return TERMINAL_RESPONSE_MARKERS.stream()
                .anyMatch(normalized::contains);
    }

    private static boolean hasVisibleElement(WebDriver driver, String selector) {
        return driver.findElements(By.cssSelector(selector))
                .stream()
                .anyMatch(WebElement::isDisplayed);
    }

    /**
     * Finds only the exact visible initial action. The caller decides whether
     * a real direct response is permitted; this extractor never clicks it.
     */
    public Optional<WebElement> findInitialResponseAction(WebDriver driver) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        return driver.findElements(By.cssSelector(INITIAL_RESPONSE_ACTION_SELECTOR))
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .filter(element -> isInitialResponseActionText(element.getText()))
                .findFirst();
    }

    private Optional<WebElement> findResponseContainer(WebDriver driver) {
        List<WebElement> containers = driver.findElements(
                By.cssSelector(RESPONSE_CONTAINER_SELECTOR)
        );

        return containers.stream()
                .filter(WebElement::isDisplayed)
                .findFirst();
    }

    private static String resolveLabel(WebDriver driver, WebElement element) {
        String id = normalize(element.getAttribute("id"));

        if (!id.isBlank()) {
            List<WebElement> labels = driver.findElements(
                    By.cssSelector("label[for='" + cssAttributeValue(id) + "']")
            );

            for (WebElement label : labels) {
                String text = normalize(label.getText());

                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        Object value = ((JavascriptExecutor) driver).executeScript(
                "const element = arguments[0];"
                        + "const parentLabel = element.closest('label');"
                        + "return parentLabel ? parentLabel.textContent : '';",
                element
        );

        return normalize(value == null ? "" : String.valueOf(value));
    }

    private static String cssAttributeValue(String value) {
        return value.replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
