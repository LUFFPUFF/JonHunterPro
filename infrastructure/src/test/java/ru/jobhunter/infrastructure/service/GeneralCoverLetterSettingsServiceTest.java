package ru.jobhunter.infrastructure.service;

import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.dto.GeneralCoverLetterSettingsDto;
import ru.jobhunter.core.application.dto.SaveGeneralCoverLetterSettingsCommand;
import ru.jobhunter.core.domain.model.GeneralCoverLetterSettings;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.repository.GeneralCoverLetterSettingsRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneralCoverLetterSettingsServiceTest {

    @Test
    void shouldCreateSettingsForUserWithoutExistingSettings() {
        InMemoryGeneralCoverLetterSettingsRepository repository =
                new InMemoryGeneralCoverLetterSettingsRepository();

        GeneralCoverLetterSettingsService service =
                new GeneralCoverLetterSettingsService(repository);

        UserId userId = userId();

        GeneralCoverLetterSettingsDto saved = service.save(
                new SaveGeneralCoverLetterSettingsCommand(
                        userId,
                        coverLetter("ООО Ромашка"),
                        true,
                        "general-cover-letter.txt"
                )
        ).join();

        assertEquals(userId, saved.userId());
        assertEquals("general-cover-letter.txt", saved.sourceFileName());
        assertTrue(saved.useWhenLlmUnavailable());
        assertEquals(coverLetter("ООО Ромашка"), saved.content());

        Optional<GeneralCoverLetterSettingsDto> loaded =
                service.findByUserId(userId).join();

        assertTrue(loaded.isPresent());
        assertEquals(saved.content(), loaded.orElseThrow().content());
    }

    @Test
    void shouldUpdateExistingSettingsWithoutCreatingSecondRecord() {
        InMemoryGeneralCoverLetterSettingsRepository repository =
                new InMemoryGeneralCoverLetterSettingsRepository();

        GeneralCoverLetterSettingsService service =
                new GeneralCoverLetterSettingsService(repository);

        UserId userId = userId();

        service.save(
                new SaveGeneralCoverLetterSettingsCommand(
                        userId,
                        coverLetter("Первая компания"),
                        true,
                        "first.txt"
                )
        ).join();

        GeneralCoverLetterSettingsDto updated = service.save(
                new SaveGeneralCoverLetterSettingsCommand(
                        userId,
                        coverLetter("Вторая компания"),
                        false,
                        null
                )
        ).join();

        assertEquals(1, repository.size());
        assertEquals(coverLetter("Вторая компания"), updated.content());
        assertFalse(updated.useWhenLlmUnavailable());
        assertEquals(null, updated.sourceFileName());
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoSettings() {
        GeneralCoverLetterSettingsService service =
                new GeneralCoverLetterSettingsService(
                        new InMemoryGeneralCoverLetterSettingsRepository()
                );

        Optional<GeneralCoverLetterSettingsDto> result =
                service.findByUserId(userId()).join();

        assertTrue(result.isEmpty());
    }

    private UserId userId() {
        return UserId.of(
                UUID.fromString("0dd7863e-9162-48de-a4d9-d1f91a87a1c4")
        );
    }

    private String coverLetter(String companyName) {
        return """
                Здравствуйте!

                Меня заинтересовала возможность присоединиться к %s.
                Я развиваюсь как Java-разработчик, работаю с Spring Boot,
                PostgreSQL и REST API, уделяю внимание качеству кода и тестированию.
                Буду рад обсудить, как мой опыт может быть полезен вашей команде.

                С уважением.
                """.formatted(companyName).strip();
    }

    private static final class InMemoryGeneralCoverLetterSettingsRepository
            implements GeneralCoverLetterSettingsRepository {

        private final Map<UserId, GeneralCoverLetterSettings> storage =
                new HashMap<>();

        @Override
        public CompletableFuture<GeneralCoverLetterSettings> save(
                GeneralCoverLetterSettings settings
        ) {
            storage.put(settings.userId(), settings);

            return CompletableFuture.completedFuture(settings);
        }

        @Override
        public CompletableFuture<Optional<GeneralCoverLetterSettings>> findByUserId(
                UserId userId
        ) {
            return CompletableFuture.completedFuture(
                    Optional.ofNullable(storage.get(userId))
            );
        }

        private int size() {
            return storage.size();
        }
    }
}