package ru.jobhunter.infrastructure.platform.habr.browser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.HabrCareerVacancyDetailsDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
public final class HabrCareerVacancyDetailsExtractor {

    static final String VACANCY_ROOT_SELECTOR = "article.vacancy-show";
    static final String JOB_POSTING_SELECTOR = "script[type='application/ld+json']";
    static final String TITLE_SELECTOR = ".vacancy-header__title h1";
    static final String DESCRIPTION_SELECTOR = ".vacancy-description__text";
    static final String SALARY_SELECTOR = ".vacancy-header__salary .predicted-salary__title";
    static final String SKILL_SELECTOR = ".vacancy-header .vacancy-meta " + ".basic-chip--color-ui-gray-4";
    static final String RESPONSE_ACTION_SELECTOR = "#create-vacancy-response button[type='submit']";
    private static final String HABR_CAREER_HOST = "career.habr.com";
    private static final String VACANCIES_PATH_PREFIX = "/vacancies/";
    private static final URI CAREER_BASE_URI = URI.create(
            "https://career.habr.com/"
    );

    private final ObjectMapper objectMapper;

    public HabrCareerVacancyDetailsExtractor(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
    }

    public HabrCareerVacancyDetailsDto extract(
            WebDriver driver,
            String expectedExternalVacancyId
    ) {
        Objects.requireNonNull(driver, "Web driver must not be null");
        String expectedId = requireExternalVacancyId(expectedExternalVacancyId);

        WebElement vacancyRoot = requireElement(
                driver,
                VACANCY_ROOT_SELECTOR,
                "Habr Career vacancy root was not found"
        );

        JsonNode jobPosting = findJobPosting(driver)
                .orElseThrow(() -> new IllegalStateException(
                        "Habr Career vacancy page does not contain JobPosting JSON-LD"
                ));

        String actualId = requiredText(
                jobPosting.at("/identifier/value").asText(),
                "Habr Career JobPosting does not contain identifier.value"
        );

        if (!expectedId.equals(actualId)) {
            throw new IllegalStateException(
                    "Habr Career JobPosting id does not match requested vacancy: "
                            + "requested="
                            + expectedId
                            + ", actual="
                            + actualId
            );
        }

        String title = firstNonBlank(
                textOfOptionalElement(vacancyRoot, TITLE_SELECTOR),
                textValue(jobPosting, "/title")
        );

        return new HabrCareerVacancyDetailsDto(
                actualId,
                requiredText(title, "Habr Career vacancy title is blank"),
                buildVacancyUrl(actualId),
                textValue(jobPosting, "/hiringOrganization/name"),
                firstNonBlank(
                        textContentOfOptionalElement(driver, vacancyRoot, DESCRIPTION_SELECTOR),
                        plainText(textValue(jobPosting, "/description"))
                ),
                extractSkills(vacancyRoot),
                location(jobPosting),
                textValue(jobPosting, "/employmentType"),
                textOfOptionalElement(vacancyRoot, SALARY_SELECTOR),
                textValue(jobPosting, "/datePosted"),
                textValue(jobPosting, "/validThrough"),
                hasAvailableResponseAction(driver)
        );
    }

    static String buildVacancyUrl(String externalVacancyId) {
        String normalizedId = requireExternalVacancyId(externalVacancyId);
        return CAREER_BASE_URI.resolve(
                VACANCIES_PATH_PREFIX.substring(1) + normalizedId
        ).toString();
    }

    static boolean isExpectedVacancyUrl(
            String finalUrl,
            String expectedExternalVacancyId
    ) {
        String expectedId = requireExternalVacancyId(expectedExternalVacancyId);

        try {
            URI uri = URI.create(requireNotBlank(
                    finalUrl,
                    "Habr Career final URL must not be blank"
            ));

            return HABR_CAREER_HOST.equalsIgnoreCase(uri.getHost())
                    && (VACANCIES_PATH_PREFIX + expectedId).equals(
                    normalize(uri.getPath())
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static Optional<JsonNode> findJobPostingNode(
            String jsonLd,
            ObjectMapper objectMapper
    ) {
        Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        String normalized = normalize(jsonLd);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        try {
            return findJobPostingNode(objectMapper.readTree(normalized));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private Optional<JsonNode> findJobPosting(WebDriver driver) {
        for (WebElement element : driver.findElements(
                By.cssSelector(JOB_POSTING_SELECTOR)
        )) {
            Optional<JsonNode> jobPosting = findJobPostingNode(
                    element.getAttribute("textContent"),
                    objectMapper
            );

            if (jobPosting.isPresent()) {
                return jobPosting;
            }
        }

        return Optional.empty();
    }

    private static Optional<JsonNode> findJobPostingNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<JsonNode> found = findJobPostingNode(child);

                if (found.isPresent()) {
                    return found;
                }
            }

            return Optional.empty();
        }

        if (!node.isObject()) {
            return Optional.empty();
        }

        if (hasJobPostingType(node.get("@type"))) {
            return Optional.of(node);
        }

        return findJobPostingNode(node.get("@graph"));
    }

    private static boolean hasJobPostingType(JsonNode typeNode) {
        if (typeNode == null || typeNode.isNull()) {
            return false;
        }

        if (typeNode.isTextual()) {
            return "JobPosting".equals(typeNode.asText());
        }

        if (typeNode.isArray()) {
            for (JsonNode type : typeNode) {
                if ("JobPosting".equals(type.asText())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<String> extractSkills(WebElement vacancyRoot) {
        List<String> skills = new ArrayList<>();

        for (WebElement element : vacancyRoot.findElements(
                By.cssSelector(SKILL_SELECTOR)
        )) {
            String skill = normalize(element.getText());

            if (!skill.isBlank()) {
                skills.add(skill);
            }
        }

        return List.copyOf(skills);
    }

    private static String location(JsonNode jobPosting) {
        JsonNode jobLocation = jobPosting.get("jobLocation");

        if (jobLocation == null || jobLocation.isNull()) {
            return "";
        }

        if (jobLocation.isArray()) {
            for (JsonNode location : jobLocation) {
                String value = locationValue(location);

                if (!value.isBlank()) {
                    return value;
                }
            }

            return "";
        }

        return locationValue(jobLocation);
    }

    private static String locationValue(JsonNode location) {
        JsonNode address = location.get("address");

        if (address == null || address.isNull()) {
            return "";
        }

        if (address.isTextual()) {
            return normalize(address.asText());
        }

        return firstNonBlank(
                textValue(address, "/addressLocality"),
                textValue(address, "/addressRegion"),
                textValue(address, "/addressCountry")
        );
    }

    private static boolean hasAvailableResponseAction(WebDriver driver) {
        return driver.findElements(By.cssSelector(RESPONSE_ACTION_SELECTOR))
                .stream()
                .anyMatch(element -> element.isDisplayed() && element.isEnabled());
    }

    private static String textContentOfOptionalElement(
            WebDriver driver,
            WebElement parent,
            String selector
    ) {
        Optional<WebElement> element = findOptionalElement(parent, selector);

        if (element.isEmpty()) {
            return "";
        }

        Object value = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].textContent;",
                element.get()
        );

        return normalize(value == null ? "" : String.valueOf(value));
    }

    private static String textOfOptionalElement(
            WebElement parent,
            String selector
    ) {
        return findOptionalElement(parent, selector)
                .map(WebElement::getText)
                .map(HabrCareerVacancyDetailsExtractor::normalize)
                .orElse("");
    }

    private static Optional<WebElement> findOptionalElement(
            WebElement parent,
            String selector
    ) {
        List<WebElement> elements = parent.findElements(By.cssSelector(selector));
        return elements.isEmpty() ? Optional.empty() : Optional.of(elements.getFirst());
    }

    private static WebElement requireElement(
            WebDriver driver,
            String selector,
            String message
    ) {
        List<WebElement> elements = driver.findElements(By.cssSelector(selector));

        if (elements.isEmpty()) {
            throw new IllegalStateException(message);
        }

        return elements.getFirst();
    }

    private static String textValue(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        return value.isMissingNode() || value.isNull()
                ? ""
                : normalize(value.asText());
    }

    private static String plainText(String value) {
        String withoutBreaks = normalize(value)
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>|</li>|</h[1-6]>", "\n");
        String withoutTags = withoutBreaks.replaceAll("<[^>]+>", " ");

        return withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replaceAll("[\\t ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String normalized = normalize(value);

            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        return "";
    }

    private static String requireExternalVacancyId(String value) {
        String normalized = requireNotBlank(
                value,
                "Habr Career external vacancy id must not be blank"
        );

        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "Habr Career external vacancy id must contain only digits"
            );
        }

        return normalized;
    }

    private static String requiredText(String value, String message) {
        return requireNotBlank(value, message);
    }

    private static String requireNotBlank(String value, String message) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
