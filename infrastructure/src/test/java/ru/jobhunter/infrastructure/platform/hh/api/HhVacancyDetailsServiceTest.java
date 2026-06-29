package ru.jobhunter.infrastructure.platform.hh.api;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.HhVacancyDetailsDto;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancyDetailsResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HhVacancyDetailsServiceTest {

    @Test
    void shouldLoadDetailedVacancyAndConvertHtmlDescription() {
        HhApiClient apiClient = mock(HhApiClient.class);

        HhApplicationTokenProperties properties =
                mock(HhApplicationTokenProperties.class);

        HhVacancyDetailsResponse response =
                new HhVacancyDetailsResponse(
                        "123456",
                        "Java Backend Developer",
                        "<p>Разработка <strong>Java</strong>-сервисов.</p>"
                                + "<p>Spring Boot и PostgreSQL.</p>",
                        "https://hh.ru/vacancy/123456",
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        false
                );

        when(properties.requireAccessToken())
                .thenReturn("application-token");

        when(apiClient.getVacancyDetailsAuthorized(
                "123456",
                "application-token"
        )).thenReturn(CompletableFuture.completedFuture(response));

        HhVacancyDetailsService service =
                new HhVacancyDetailsService(
                        apiClient,
                        properties
                );

        HhVacancyDetailsDto result = service.getDetails("123456").join();

        assertEquals("123456", result.externalId());
        assertEquals("Java Backend Developer", result.name());
        assertEquals(
                "Разработка Java-сервисов. Spring Boot и PostgreSQL.",
                result.description()
        );
        assertEquals(
                "https://hh.ru/vacancy/123456",
                result.vacancyUrl()
        );
        assertFalse(result.responseLetterRequired());

        verify(apiClient).getVacancyDetailsAuthorized(
                "123456",
                "application-token"
        );
    }

    @Test
    void shouldRejectBlankVacancyId() {
        HhVacancyDetailsService service =
                new HhVacancyDetailsService(
                        mock(HhApiClient.class),
                        mock(HhApplicationTokenProperties.class)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getDetails("   ")
        );
    }
}