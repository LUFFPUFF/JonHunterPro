package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.GeneralCoverLetterSettingsDto;
import ru.jobhunter.core.application.usecase.coverletter.GetGeneralCoverLetterSettingsUseCase;
import ru.jobhunter.core.domain.model.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneralCoverLetterFallbackResolverTest {

    @Test
    void shouldReturnValidatedFallbackWhenEnabled() {
        UserId userId = userId();

        GeneralCoverLetterFallbackResolver resolver =
                resolverFor(
                        settings(
                                userId,
                                validCoverLetter(),
                                true
                        )
                );

        Optional<String> result = resolver.resolve(userId).join();

        assertTrue(result.isPresent());
        assertEquals(validCoverLetter(), result.orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenFallbackIsDisabled() {
        UserId userId = userId();

        GeneralCoverLetterFallbackResolver resolver =
                resolverFor(
                        settings(
                                userId,
                                validCoverLetter(),
                                false
                        )
                );

        Optional<String> result = resolver.resolve(userId).join();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenFallbackDoesNotPassQualityGate() {
        UserId userId = userId();

        GeneralCoverLetterFallbackResolver resolver =
                resolverFor(
                        settings(
                                userId,
                                "Здравствуйте. Короткое письмо.",
                                true
                        )
                );

        Optional<String> result = resolver.resolve(userId).join();

        assertTrue(result.isEmpty());
    }

    private GeneralCoverLetterFallbackResolver resolverFor(
            GeneralCoverLetterSettingsDto settings
    ) {
        GetGeneralCoverLetterSettingsUseCase useCase =
                userId -> CompletableFuture.completedFuture(
                        Optional.of(settings)
                );

        return new GeneralCoverLetterFallbackResolver(useCase);
    }

    private GeneralCoverLetterSettingsDto settings(
            UserId userId,
            String content,
            boolean enabled
    ) {
        Instant timestamp = Instant.parse("2026-06-28T12:00:00Z");

        return new GeneralCoverLetterSettingsDto(
                userId,
                content,
                enabled,
                "general-cover-letter.txt",
                timestamp,
                timestamp
        );
    }

    private UserId userId() {
        return UserId.of(
                UUID.fromString("8991a511-a295-4f96-aead-813fe8cc1f17")
        );
    }

    private String validCoverLetter() {
        return """
                Здравствуйте!

                Меня заинтересовала возможность присоединиться к вашей команде.
                Я развиваюсь как Java-разработчик, работаю со Spring Boot,
                PostgreSQL, REST API и уделяю внимание качеству кода.
                Буду рад обсудить, как мой опыт может быть полезен компании.

                С уважением.
                """.strip();
    }
}