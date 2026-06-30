package ru.jobhunter.infrastructure.platform.habr.browser;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.jobhunter.core.application.dto.HabrCareerVisibleVacancyDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
public final class HabrCareerVacancyCardExtractor {

    static final String VACANCY_CARD_SELECTOR = "[data-vacancy-card]";

    private static final Logger log = LoggerFactory.getLogger(
            HabrCareerVacancyCardExtractor.class
    );

    private static final String HABR_CAREER_HOST = "career.habr.com";
    private static final String VACANCIES_PATH_PREFIX = "/vacancies/";
    private static final URI VACANCIES_URI = URI.create(
            "https://career.habr.com/vacancies"
    );

    private static final String TITLE_LINK_SELECTOR =
            ".vacancy-card__title-link";
    private static final String COMPANY_LINK_SELECTOR =
            ".vacancy-card__company a";
    private static final String PUBLISHED_AT_SELECTOR = "time[datetime]";
    private static final String SALARY_PRIMARY_SELECTOR =
            ".vacancy-card__salary h4";
    private static final String SALARY_FALLBACK_SELECTOR =
            ".vacancy-card__salary";
    private static final String META_CHIP_SELECTOR =
            ".vacancy-card__meta .basic-chip";
    private static final String META_TEXT_SELECTOR =
            ".chip-with-icon__text";
    private static final String SKILL_SELECTOR =
            ".vacancy-card__skills-chip";
    private static final String SVG_USE_SELECTOR = "use";

    public List<HabrCareerVisibleVacancyDto> extractVisibleVacancies(
            WebDriver driver
    ) {
        Objects.requireNonNull(driver, "Web driver must not be null");

        List<HabrCareerVisibleVacancyDto> vacancies = new ArrayList<>();

        for (WebElement card : driver.findElements(
                By.cssSelector(VACANCY_CARD_SELECTOR)
        )) {
            extractVacancyCard(card).ifPresent(vacancies::add);
        }

        return List.copyOf(vacancies);
    }

    private Optional<HabrCareerVisibleVacancyDto> extractVacancyCard(
            WebElement card
    ) {
        String externalVacancyId = normalize(
                card.getAttribute("data-vacancy-id")
        );

        try {
            if (externalVacancyId.isBlank()) {
                throw new IllegalArgumentException(
                        "Habr Career vacancy card does not contain "
                                + "data-vacancy-id"
                );
            }

            WebElement titleLink = requireElement(
                    card,
                    TITLE_LINK_SELECTOR,
                    "Habr Career vacancy card does not contain a title link: "
                            + "id=" + externalVacancyId
            );

            String title = requireNotBlank(
                    textOf(titleLink),
                    "Habr Career vacancy title is blank: id="
                            + externalVacancyId
            );

            VacancyMetadata metadata = extractMetadata(card);

            return Optional.of(new HabrCareerVisibleVacancyDto(
                    externalVacancyId,
                    title,
                    resolveVacancyUrl(titleLink.getAttribute("href")),
                    textOfOptionalElement(card, COMPANY_LINK_SELECTOR),
                    metadata.city(),
                    metadata.qualification(),
                    metadata.workFormat(),
                    firstNonBlank(
                            textOfOptionalElement(
                                    card,
                                    SALARY_PRIMARY_SELECTOR
                            ),
                            textOfOptionalElement(
                                    card,
                                    SALARY_FALLBACK_SELECTOR
                            )
                    ),
                    extractSkills(card),
                    publishedAt(card)
            ));
        } catch (RuntimeException exception) {
            log.warn(
                    "Skipping incomplete Habr Career vacancy card: id={}, "
                            + "reason={}",
                    externalVacancyId.isBlank() ? "unknown" : externalVacancyId,
                    rootMessage(exception)
            );

            return Optional.empty();
        }
    }

    private VacancyMetadata extractMetadata(WebElement card) {
        String qualification = "";
        String workFormat = "";
        String city = "";

        for (WebElement chip : card.findElements(
                By.cssSelector(META_CHIP_SELECTOR)
        )) {
            String chipText = textOfOptionalElement(chip, META_TEXT_SELECTOR);

            if (chipText.isBlank()) {
                continue;
            }

            switch (resolveMetaField(svgIconHref(chip))) {
                case QUALIFICATION -> qualification = chipText;
                case WORK_FORMAT -> workFormat = chipText;
                case CITY -> city = chipText;
                case UNKNOWN -> {
                    // Unknown Habr Career metadata is deliberately ignored here.
                }
            }
        }

        return new VacancyMetadata(qualification, workFormat, city);
    }

    private List<String> extractSkills(WebElement card) {
        List<String> skills = new ArrayList<>();

        for (WebElement skill : card.findElements(By.cssSelector(SKILL_SELECTOR))) {
            String value = textOf(skill);

            if (!value.isBlank()) {
                skills.add(value);
            }
        }

        return List.copyOf(skills);
    }

    private String publishedAt(WebElement card) {
        Optional<WebElement> element = findOptionalElement(
                card,
                PUBLISHED_AT_SELECTOR
        );

        if (element.isEmpty()) {
            return "";
        }

        String dateTime = normalize(element.get().getAttribute("datetime"));
        return dateTime.isBlank() ? textOf(element.get()) : dateTime;
    }

    static String resolveVacancyUrl(String href) {
        URI resolved = VACANCIES_URI.resolve(requireNotBlank(
                href,
                "Habr Career vacancy href must not be blank"
        ));
        String host = normalize(resolved.getHost()).toLowerCase(Locale.ROOT);
        String path = normalize(resolved.getPath());

        if (!HABR_CAREER_HOST.equals(host)
                || !path.startsWith(VACANCIES_PATH_PREFIX)
                || path.length() <= VACANCIES_PATH_PREFIX.length()) {
            throw new IllegalArgumentException(
                    "Unexpected Habr Career vacancy URL: " + resolved
            );
        }

        return resolved.toString();
    }

    static MetaField resolveMetaField(String iconHref) {
        String normalized = normalize(iconHref).toLowerCase(Locale.ROOT);

        if (normalized.contains("#grade")) {
            return MetaField.QUALIFICATION;
        }

        if (normalized.contains("#format")) {
            return MetaField.WORK_FORMAT;
        }

        if (normalized.contains("#placemark")) {
            return MetaField.CITY;
        }

        return MetaField.UNKNOWN;
    }

    static String textOf(WebElement element) {
        Objects.requireNonNull(element, "Web element must not be null");

        return firstNonBlank(
                element.getText(),
                element.getAttribute("textContent"),
                element.getAttribute("innerText"),
                element.getAttribute("aria-label")
        );
    }

    private String svgIconHref(WebElement chip) {
        for (WebElement useElement : chip.findElements(
                By.cssSelector(SVG_USE_SELECTOR)
        )) {
            String href = firstNonBlank(
                    useElement.getAttribute("href"),
                    useElement.getAttribute("xlink:href")
            );

            if (!href.isBlank()) {
                return href;
            }
        }

        return "";
    }

    private static WebElement requireElement(
            WebElement parent,
            String selector,
            String message
    ) {
        return findOptionalElement(parent, selector)
                .orElseThrow(() -> new IllegalStateException(message));
    }

    private static Optional<WebElement> findOptionalElement(
            WebElement parent,
            String selector
    ) {
        List<WebElement> elements = parent.findElements(By.cssSelector(selector));
        return elements.isEmpty() ? Optional.empty() : Optional.of(elements.getFirst());
    }

    private static String textOfOptionalElement(
            WebElement parent,
            String selector
    ) {
        return findOptionalElement(parent, selector)
                .map(HabrCareerVacancyCardExtractor::textOf)
                .orElse("");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);

            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        return "";
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

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    enum MetaField {
        QUALIFICATION,
        WORK_FORMAT,
        CITY,
        UNKNOWN
    }

    private record VacancyMetadata(
            String qualification,
            String workFormat,
            String city
    ) {
    }
}
