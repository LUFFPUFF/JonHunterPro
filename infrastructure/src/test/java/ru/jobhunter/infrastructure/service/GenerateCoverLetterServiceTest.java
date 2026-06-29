package ru.jobhunter.infrastructure.service;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationRequest;
import ru.jobhunter.core.application.port.out.llm.LlmGenerationResponse;
import ru.jobhunter.core.application.port.out.llm.LlmPort;
import ru.jobhunter.core.application.port.out.llm.LlmUsage;
import ru.jobhunter.core.application.usecase.coverletter.GenerateCoverLetterCommand;
import ru.jobhunter.core.application.usecase.coverletter.GeneratedCoverLetterDto;
import ru.jobhunter.core.domain.model.UserId;
import ru.jobhunter.core.domain.model.VacancySource;
import ru.jobhunter.infrastructure.llm.routing.LlmProviderUnavailableException;
import ru.jobhunter.infrastructure.prompt.ClasspathJinjaPromptTemplateRenderer;
import ru.jobhunter.infrastructure.prompt.PromptTemplateRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCoverLetterServiceTest {

    @Test
    void shouldGenerateCoverLetterUsingLlmPortAndJinjaTemplates() {
        AtomicReference<LlmGenerationRequest> capturedRequest =
                new AtomicReference<>();

        GenerateCoverLetterService service = getGenerateCoverLetterService(capturedRequest);


        GenerateCoverLetterCommand command = command();

        GeneratedCoverLetterDto result = service.generate(command).join();

        assertEquals("134229036", result.vacancyId());
        assertEquals("Java-разработчик", result.vacancyTitle());
        assertEquals("Тестовая компания", result.companyName());
        assertEquals("test-provider", result.provider());
        assertEquals("test-model", result.model());
        assertTrue(result.content().contains("Java"));
        assertFalse(result.content().isBlank());

        LlmGenerationRequest request = capturedRequest.get();

        assertNotNull(request);
        assertEquals("generate-cover-letter", request.useCase());
        assertEquals(2, request.messages().size());

        String systemPrompt = request.messages().get(0).content();
        String userPrompt = request.messages().get(1).content();

        assertTrue(systemPrompt.contains("карьерный ассистент"));
        assertTrue(systemPrompt.contains("1400 символов"));

        assertTrue(userPrompt.contains("Java-разработчик"));
        assertTrue(userPrompt.contains("Тестовая компания"));
        assertTrue(userPrompt.contains("Java, Spring Boot, PostgreSQL"));
        assertTrue(
                userPrompt.contains(
                        "Опыт Java, Spring Boot, PostgreSQL"
                )
        );


        assertFalse(userPrompt.contains("{{ vacancy_title }}"));
        assertFalse(userPrompt.contains("{{ company_name }}"));
        assertFalse(userPrompt.contains("{{ resume_text }}"));
    }

    private static @NonNull GenerateCoverLetterService getGenerateCoverLetterService(AtomicReference<LlmGenerationRequest> capturedRequest) {
        LlmPort llmPort = request -> {
            capturedRequest.set(request);

            return CompletableFuture.completedFuture(
                    new LlmGenerationResponse(
                            "test-provider",
                            "test-model",
                            """
                            Здравствуйте! Меня заинтересовала вакансия
                            Java-разработчика. Мой опыт с Java и Spring Boot
                            соответствует задачам проекта.
                            """,
                            LlmUsage.unknown()
                    )
            );
        };

        PromptTemplateRenderer promptTemplateRenderer =
                new ClasspathJinjaPromptTemplateRenderer();

        return new GenerateCoverLetterService(
                llmPort,
                promptTemplateRenderer
        );
    }

    @Test
    void shouldRetryWithCompactOptionsWhenFirstResponseIsPlainReasoning() {
        List<LlmGenerationRequest> capturedRequests = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();

        LlmPort llmPort = request -> {
            capturedRequests.add(request);

            String content = calls.getAndIncrement() == 0
                    ? """
                Хорошо, мне нужно подготовить сопроводительное письмо.
                Сначала я должен изучить требования вакансии.
                Вакансия требует Java и Spring Boot.
                У кандидата есть опыт разработки backend-систем.
                """
                    : """
                Здравствуйте! Меня заинтересовала вакансия Java-разработчика.
                Более трёх лет разрабатываю backend-сервисы на Java и Spring Boot,
                работал с PostgreSQL, интеграциями и сопровождением production-систем.

                Буду рад применить этот опыт в вашей команде и обсудить,
                какие задачи будут приоритетными на старте.
                """;

            return CompletableFuture.completedFuture(
                    new LlmGenerationResponse(
                            "ollama",
                            "qwen3:4b",
                            content,
                            LlmUsage.unknown()
                    )
            );
        };

        GenerateCoverLetterService service =
                new GenerateCoverLetterService(
                        llmPort,
                        new ClasspathJinjaPromptTemplateRenderer()
                );

        GeneratedCoverLetterDto result = service.generate(command()).join();

        assertEquals(2, capturedRequests.size());
        assertEquals(
                0.3,
                capturedRequests.get(0).options().temperature()
        );
        assertEquals(
                420,
                capturedRequests.get(0).options().maxTokens()
        );
        assertEquals(
                0.0,
                capturedRequests.get(1).options().temperature()
        );
        assertEquals(
                300,
                capturedRequests.get(1).options().maxTokens()
        );

        assertTrue(result.content().startsWith("Здравствуйте!"));
        assertFalse(result.content().contains("Мне нужно подготовить"));
    }

    @Test
    void shouldRemoveCompletedThinkBlockBeforeReturningCoverLetter() {
        LlmPort llmPort = request -> CompletableFuture.completedFuture(
                new LlmGenerationResponse(
                        "ollama",
                        "qwen3:4b",
                        """
                        <think>
                        Нужно сравнить резюме с вакансией и подобрать факты.
                        </think>
    
                        Здравствуйте! Меня заинтересовала вакансия Java-разработчика.
                        Имею опыт разработки backend-систем на Java и Spring Boot,
                        работал с PostgreSQL и сопровождением production-сервисов.
    
                        Буду рад обсудить, как мой опыт может быть полезен команде.
                        """,
                        LlmUsage.unknown()
                )
        );

        GenerateCoverLetterService service =
                new GenerateCoverLetterService(
                        llmPort,
                        new ClasspathJinjaPromptTemplateRenderer()
                );

        GeneratedCoverLetterDto result = service.generate(command()).join();

        assertTrue(result.content().startsWith("Здравствуйте!"));
        assertFalse(result.content().contains("<think>"));
        assertFalse(result.content().contains("Нужно сравнить резюме"));
    }

    @Test
    void shouldFailBeforeReturningUnsafeLetterWhenRetryIsAlsoReasoning() {
        AtomicInteger calls = new AtomicInteger();

        LlmPort llmPort = request -> {
            calls.incrementAndGet();

            return CompletableFuture.completedFuture(
                    new LlmGenerationResponse(
                            "ollama",
                            "qwen3:4b",
                            """
                            Хорошо, мне нужно подготовить письмо.
                            Сначала я должен изучить вакансию.
                            У кандидата есть опыт Java.
                            Вакансия требует Spring Boot.
                            """,
                            LlmUsage.unknown()
                    )
            );
        };

        GenerateCoverLetterService service =
                new GenerateCoverLetterService(
                        llmPort,
                        new ClasspathJinjaPromptTemplateRenderer()
                );

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> service.generate(command()).join()
        );

        assertTrue(
                exception.getCause().getMessage().contains(
                        "planning or internal reasoning"
                )
        );
        assertEquals(2, calls.get());

    }

    private GenerateCoverLetterCommand command() {
        return new GenerateCoverLetterCommand(
                UserId.of(
                        UUID.fromString(
                                "7b908e12-fe65-4aa2-b78c-a9c002ad6d2e"
                        )
                ),
                VacancySource.HH_RU,
                "134229036",
                "Java-разработчик",
                "Тестовая компания",
                "https://hh.ru/vacancy/134229036",
                """
                Требуется Java-разработчик.
                Java, Spring Boot, PostgreSQL.
                """,
                """
                Опыт Java, Spring Boot, PostgreSQL.
                Разработка desktop-приложений и backend-сервисов.
                """
        );
    }

    @Test
    void shouldUseRepairPromptWhenFirstGenerationContainsReasoning() {
        List<LlmGenerationRequest> requests = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();

        LlmPort llmPort = request -> {
            requests.add(request);

            String content = calls.getAndIncrement() == 0
                    ? """
                Хорошо, мне нужно подготовить сопроводительное письмо.
                Вакансия требует Java и Spring Boot.
                У кандидата есть релевантный опыт.
                Следует выбрать несколько фактов из резюме.
                """
                    : """
                Здравствуйте! Меня заинтересовала вакансия Java-разработчика.
                Более трёх лет работаю с Java и Spring Boot, разрабатывал
                backend-сервисы, интеграции и решения для production-среды.

                Буду рад обсудить, как мой опыт может быть полезен вашей команде.
                """;

            return CompletableFuture.completedFuture(
                    new LlmGenerationResponse(
                            "ollama",
                            "qwen3:4b",
                            content,
                            LlmUsage.unknown()
                    )
            );
        };

        GenerateCoverLetterService service =
                new GenerateCoverLetterService(
                        llmPort,
                        new ClasspathJinjaPromptTemplateRenderer()
                );

        GeneratedCoverLetterDto result = service.generate(
                command()
        ).join();

        assertEquals(2, requests.size());

        assertFalse(
                requests.get(0).excludesProvider("ollama")
        );

        assertTrue(
                requests.get(1).excludesProvider("ollama")
        );

        assertEquals(
                0.3,
                requests.get(0).options().temperature()
        );
        assertEquals(
                0.0,
                requests.get(1).options().temperature()
        );

        assertTrue(
                requests.get(1)
                        .messages()
                        .getFirst()
                        .content()
                        .contains("первый символ ответа")
        );

        assertTrue(result.content().startsWith("Здравствуйте!"));
    }

    @Test
    void shouldExposeRepeatedUnsafeGenerationAsTemporaryLlmFailure() {
        LlmPort llmPort = request -> CompletableFuture.completedFuture(
                new LlmGenerationResponse(
                        "ollama",
                        "qwen3:4b",
                        """
                        Хорошо, мне нужно подготовить сопроводительное письмо.
                        Вакансия требует Java.
                        У кандидата есть опыт Java.
                        Следует выбрать подтверждённые факты.
                        """,
                        LlmUsage.unknown()
                )
        );

        GenerateCoverLetterService service =
                new GenerateCoverLetterService(
                        llmPort,
                        new ClasspathJinjaPromptTemplateRenderer()
                );

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> service.generate(command()).join()
        );

        assertInstanceOf(
                LlmProviderUnavailableException.class,
                exception.getCause()
        );
    }


}