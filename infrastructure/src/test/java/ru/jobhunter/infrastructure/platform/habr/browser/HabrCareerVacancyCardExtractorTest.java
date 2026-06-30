package ru.jobhunter.infrastructure.platform.habr.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.jobhunter.core.application.dto.HabrCareerVisibleVacancyDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HabrCareerVacancyCardExtractorTest {

    @Test
    void resolvesRelativeVacancyUrlAgainstHabrCareerHost() {
        String result = HabrCareerVacancyCardExtractor.resolveVacancyUrl(
                "/vacancies/1000161662"
        );

        assertEquals(
                "https://career.habr.com/vacancies/1000161662",
                result
        );
    }

    @Test
    void rejectsUrlOutsideHabrCareerVacancies() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HabrCareerVacancyCardExtractor.resolveVacancyUrl(
                        "https://example.com/vacancies/1000161662"
                )
        );
    }

    @Test
    void resolvesKnownMetadataIcons() {
        assertEquals(
                HabrCareerVacancyCardExtractor.MetaField.QUALIFICATION,
                HabrCareerVacancyCardExtractor.resolveMetaField(
                        "/images/icons-sprite.svg#grade"
                )
        );
        assertEquals(
                HabrCareerVacancyCardExtractor.MetaField.WORK_FORMAT,
                HabrCareerVacancyCardExtractor.resolveMetaField(
                        "/images/icons-sprite.svg#format"
                )
        );
        assertEquals(
                HabrCareerVacancyCardExtractor.MetaField.CITY,
                HabrCareerVacancyCardExtractor.resolveMetaField(
                        "/images/icons-sprite.svg#placemark"
                )
        );
    }

    @Test
    void fallsBackToTextContentWhenSeleniumVisibleTextIsBlank() {
        WebElement titleLink = mock(WebElement.class);
        when(titleLink.getText()).thenReturn("");
        when(titleLink.getAttribute("textContent"))
                .thenReturn("Java Developer");

        assertEquals(
                "Java Developer",
                HabrCareerVacancyCardExtractor.textOf(titleLink)
        );
    }

    @Test
    void skipsIncompleteCardAndKeepsOtherVacanciesVisible() {
        WebDriver driver = mock(WebDriver.class);
        WebElement incompleteCard = mock(WebElement.class);
        WebElement incompleteTitle = mock(WebElement.class);
        WebElement validCard = mock(WebElement.class);
        WebElement validTitle = mock(WebElement.class);

        when(driver.findElements(any(By.class)))
                .thenReturn(List.of(incompleteCard, validCard));

        when(incompleteCard.getAttribute("data-vacancy-id"))
                .thenReturn("1000161325");
        configureCard(incompleteCard, incompleteTitle);
        when(incompleteTitle.getText()).thenReturn("");
        when(incompleteTitle.getAttribute("textContent")).thenReturn("");
        when(incompleteTitle.getAttribute("innerText")).thenReturn("");
        when(incompleteTitle.getAttribute("aria-label")).thenReturn("");

        when(validCard.getAttribute("data-vacancy-id"))
                .thenReturn("1000167360");
        configureCard(validCard, validTitle);
        when(validTitle.getText()).thenReturn("Старший Java разработчик");
        when(validTitle.getAttribute("href"))
                .thenReturn("/vacancies/1000167360");

        List<HabrCareerVisibleVacancyDto> vacancies =
                new HabrCareerVacancyCardExtractor()
                        .extractVisibleVacancies(driver);

        assertEquals(1, vacancies.size());
        assertEquals("1000167360", vacancies.getFirst().externalVacancyId());
        assertEquals("Старший Java разработчик", vacancies.getFirst().title());
    }

    private static void configureCard(
            WebElement card,
            WebElement titleLink
    ) {
        when(card.findElements(any(By.class))).thenAnswer(invocation -> {
            String selector = invocation.getArgument(0).toString();

            if (selector.contains(".vacancy-card__title-link")) {
                return List.of(titleLink);
            }

            return List.of();
        });
    }
}
