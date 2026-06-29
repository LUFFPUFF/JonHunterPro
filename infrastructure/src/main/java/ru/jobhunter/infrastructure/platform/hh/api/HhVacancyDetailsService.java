package ru.jobhunter.infrastructure.platform.hh.api;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.jobhunter.core.application.dto.HhVacancyDetailsDto;
import ru.jobhunter.core.application.usecase.integration.GetHhVacancyDetailsUseCase;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhDictionaryItemResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhEmployerShortResponse;
import ru.jobhunter.infrastructure.platform.hh.api.dto.HhVacancyDetailsResponse;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public final class HhVacancyDetailsService
        implements GetHhVacancyDetailsUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(HhVacancyDetailsService.class);

    private static final String FALLBACK_VACANCY_URL_TEMPLATE =
            "https://hh.ru/vacancy/%s";

    private final HhApiClient apiClient;
    private final HhApplicationTokenProperties applicationTokenProperties;

    public HhVacancyDetailsService(
            HhApiClient apiClient,
            HhApplicationTokenProperties applicationTokenProperties
    ) {
        this.apiClient = Objects.requireNonNull(
                apiClient,
                "HH API client must not be null"
        );

        this.applicationTokenProperties = Objects.requireNonNull(
                applicationTokenProperties,
                "HH application token properties must not be null"
        );
    }

    @Override
    public CompletableFuture<HhVacancyDetailsDto> getDetails(
            String externalVacancyId
    ) {
        String normalizedVacancyId = requireNotBlank(
                externalVacancyId,
                "HH vacancy external id must not be blank"
        );

        log.info(
                "Loading detailed HH.ru vacancy for cover letter generation: vacancyId={}",
                normalizedVacancyId
        );

        String applicationAccessToken =
                applicationTokenProperties.requireAccessToken();

        return apiClient.getVacancyDetailsAuthorized(
                        normalizedVacancyId,
                        applicationAccessToken
                )
                .thenApply(this::toDto);
    }

    private HhVacancyDetailsDto toDto(
            HhVacancyDetailsResponse response
    ) {
        Objects.requireNonNull(
                response,
                "HH vacancy details response must not be null"
        );

        String vacancyId = requireNotBlank(
                response.id(),
                "HH vacancy details do not contain id"
        );

        return new HhVacancyDetailsDto(
                vacancyId,
                requireNotBlank(
                        response.name(),
                        "HH vacancy details do not contain name"
                ),
                employerNameOf(response.employer()),
                nameOf(response.area()),
                firstNonBlank(
                        response.alternateUrl(),
                        FALLBACK_VACANCY_URL_TEMPLATE.formatted(vacancyId)
                ),
                htmlToPlainText(response.description()),
                skillNames(response.keySkills()),
                nameOf(response.experience()),
                nameOf(response.employment()),
                nameOf(response.schedule()),
                Boolean.TRUE.equals(response.responseLetterRequired())
        );
    }

    private String htmlToPlainText(String html) {
        String plainText = Jsoup.parseBodyFragment(
                        html == null ? "" : html
                )
                .text()
                .replace('\u00A0', ' ')
                .replaceAll("\\s{2,}", " ")
                .strip();

        if (plainText.isBlank()) {
            throw new IllegalStateException(
                    "HH vacancy details do not contain a textual description"
            );
        }

        return plainText;
    }

    private List<String> skillNames(
            List<HhDictionaryItemResponse> skills
    ) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        return skills.stream()
                .filter(Objects::nonNull)
                .map(HhDictionaryItemResponse::name)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String employerNameOf(HhEmployerShortResponse employer) {
        return employer == null
                ? null
                : normalize(employer.name());
    }

    private String nameOf(HhDictionaryItemResponse item) {
        return item == null
                ? null
                : normalize(item.name());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }

        return second == null ? null : second.trim();
    }

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}