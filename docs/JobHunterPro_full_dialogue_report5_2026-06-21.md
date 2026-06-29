# JobHunterPro Desktop Application — полный отчёт по текущему чату

**Дата составления:** 2026-06-21  
**Проект:** `JobHunterPro Desktop Application`  
**Локальный путь:** `C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application`  
**Назначение:** передать контекст следующему чату без потери технических решений, текущего состояния реализации, подтверждённых результатов и ограничений.

> **Правило для следующего чата:** не угадывать структуру проекта и не писать патчи для непоказанных актуальных файлов. Проект активно менялся. Если нужен конкретный класс, DTO, тест, YAML, FXML, миграция или метод — попросить пользователя приложить его актуальную версию.

---

## 1. Как начать новый чат

В новый чат нужно приложить этот файл и написать:

```text
Продолжаем разработку JobHunterPro Desktop Application.

Прочитай приложенный markdown-отчёт и используй его как основной контекст.
При необходимости также изучи предыдущие приложенные отчёты JobHunterPro.

Не гадай по отсутствующим файлам: если нужен актуальный код, тест, конфигурация или FXML, попроси меня приложить файл.

Текущая точка остановки:
LLM уже умеет генерировать и заполнять ответы на текстовые вопросы HH.ru в режиме review, но финальная отправка анкеты намеренно не выполняется.
Нужно улучшить качество ответов через профиль фактов кандидата и не вставлять в форму технические фразы вида «требует уточнения кандидатом».
```

---

## 2. Ссылка на чат

Я не могу получить реальную ссылку Share на текущий чат изнутри среды ChatGPT.

```text
Ссылка на текущий чат: https://chatgpt.com/c/6a3190f0-c2dc-83eb-85fb-06f200818767
```

---

## 3. Архитектурный и продуктовый контекст

### 3.1. Главный продуктовый принцип

`JobHunterPro` должен оставаться приложением про настоящие **автоотклики**.

```text
Основной сценарий:
поиск вакансий → очередь → подготовка → автоматический отклик.

Не превращать продукт в основной ручной сценарий
«открой вакансию и откликнись сам».

Ручная проверка допустима как временный safety/review режим,
особенно для нестандартных анкет работодателя.
```

### 3.2. Технологический стек

```text
Java 21
JavaFX 21
Spring Boot 3.5.x headless
Maven multi-module
PostgreSQL 16 на localhost:5434
Flyway
Hibernate/JPA
OkHttp 5.4.0
Jackson
Selenium WebDriver
JUnit 5
MockWebServer
PDFBox 3.0.7
jsoup
```

### 3.3. Модули

```text
jobhunter-pro/
├── core/
├── infrastructure/
├── ui/
└── app/
```

Правила зависимостей:

```text
core:
- домен, DTO, use case, outbound ports;
- не зависит от Spring, JPA, JavaFX, Selenium, OkHttp.

infrastructure:
- реализует outbound ports;
- HTTP, OpenRouter, Selenium, persistence, PDF extraction.

ui:
- JavaFX;
- использует use case из core;
- не зависит напрямую от infrastructure.

app:
- Spring Boot bootstrap и application.yaml.
```

---

## 4. Исторический контекст до этого чата

Предыдущие отчёты, которые желательно приложить в новый чат при необходимости:

```text
JobHunterPro_Master_Prompt
JobHunterPro_dialogue_report1
JobHunterPro_full_dialogue_report2_2026-06-14
JobHunterPro_full_dialogue_report3_2026-06-16
JobHunterPro_full_dialogue_report4_2026-06-16
```

К началу текущего чата уже было подтверждено:

```text
- HH.ru developer application одобрено.
- Real HH OAuth работает.
- Redirect URI: jobhunterpro://oauth/hh/callback.
- OAuth token сохраняется в external_auth_tokens.
- HH connection status = CONNECTED.
- HH API /me работает.
- Habr Career OAuth/API пока отложены из-за внешних ограничений.
- Очередь автооткликов и READY-фильтрация существуют.
```

---

## 5. Итог текущего чата в одном блоке

В текущем чате было сделано:

```text
1. Подключён LLM-порт и OpenRouter-адаптер.
2. Реализована генерация сопроводительных писем на основе вакансии и primary PDF-резюме.
3. Реализован primary resume pipeline: загрузка PDF, извлечение текста, хранение и выбор актуального резюме.
4. HH vacancy details подключены через API для качественной генерации письма.
5. Browser-agent автоотклика доработан:
   - PREFLIGHT и EXECUTE режимы;
   - единый executionId и Selenium sessionId;
   - диагностика HTML + PNG по executionId;
   - безопасный preflight;
   - реальная отправка отклика;
   - отправка сопроводительного письма в post-response форме HH.
6. Подключена LLM для текстовых вопросов работодателя HH.ru.
7. LLM теперь возвращает структурированный JSON для анкет.
8. Selenium извлекает текстовые вопросы, заполняет ответы и сохраняет диагностику.
9. Финальная отправка анкеты пока специально не выполняется:
   QUESTIONNAIRE_FILLED_REVIEW_REQUIRED.
```

Текущий главный результат:

```text
Полный автоотклик с сопроводительным письмом работает end-to-end.

Анкеты с текстовыми вопросами:
- вопросы обнаруживаются;
- LLM генерирует ответы;
- Selenium вставляет ответы в поля;
- финальный submit анкеты пока НЕ нажимается;
- статус остаётся review-required.
```

---

## 6. LLM: базовые контракты

Добавлены generic contracts в:

```text
core/application/port/out/llm/
```

Ключевые классы:

```text
LlmPort
LlmRole
LlmMessage
LlmGenerationRequest
LlmGenerationResponse
LlmGenerationOptions
LlmUsage
LlmResponseFormat
```

### 6.1. LlmPort

```java
CompletableFuture<LlmGenerationResponse> generate(
        LlmGenerationRequest request
);
```

### 6.2. Форматы ответа

Добавлен enum:

```java
public enum LlmResponseFormat {
    TEXT,
    JSON_OBJECT
}
```

`LlmGenerationOptions` теперь поддерживает:

```text
temperature
maxTokens
responseFormat
```

Основные фабрики:

```text
balanced()            -> текстовый режим для сопроводительных писем
deterministic()       -> текстовый режим с низкой temperature
deterministicJson()   -> JSON_OBJECT для ответов на анкеты
```

---

## 7. OpenRouter LLM integration

### 7.1. Реализованные классы

В `infrastructure/llm/openrouter/`:

```text
OpenRouterProperties
OpenRouterLlmClient
OpenRouterLlmAdapter
OpenRouterLlmConfiguration
OpenRouterLlmException
DTO:
- OpenRouterChatMessage
- OpenRouterChatRequest
- OpenRouterChatResponse
- OpenRouterChoice
- OpenRouterUsage
- OpenRouterErrorResponse
- OpenRouterResponseFormat
```

### 7.2. Важные решения

```text
- Provider: OpenRouter.
- Primary model: openrouter/free.
- Не хранить реальные API keys в application.yaml и Git.
- Keys задаются через IntelliJ Run Configuration -> Environment variables.
- Для нестабильных free-моделей предусмотрен общая fallback-механика OpenRouterLlmAdapter.
- На практике openrouter/free иногда может вернуть пустой content или нестрогий формат.
```

### 7.3. JSON structured output для анкеты

Для use case `answer-hh-questionnaire` добавлен:

```json
{
  "response_format": {
    "type": "json_object"
  }
}
```

Техническая реализация:

```text
LlmGenerationOptions.responseFormat = JSON_OBJECT
→ OpenRouterLlmAdapter.mapResponseFormat(...)
→ OpenRouterChatRequest.response_format
→ OpenRouterResponseFormat("json_object")
```

Это исправило предыдущую ошибку:

```text
LLM questionnaire response does not contain JSON object
```

### 7.4. OkHttp dependency injection

При включении OpenRouter в Spring появились два `OkHttpClient`:

```text
okHttpClient
openRouterOkHttpClient
```

Исправление:

```text
- основной global okHttpClient помечен @Primary;
- OpenRouter client получает @Qualifier("openRouterOkHttpClient").
```

Не удалять это решение без необходимости: иначе возникнет неоднозначная Spring-инъекция.

---

## 8. Environment variables

Пример актуальных значений без секретов:

```text
OPENROUTER_ENABLED=true
OPENROUTER_API_KEY=<SECRET>
OPENROUTER_PRIMARY_MODEL=openrouter/free
OPENROUTER_FALLBACK_MODELS=
OPENROUTER_HTTP_REFERER=https://jobhunterpro.local
OPENROUTER_APPLICATION_TITLE=JobHunterPro

HH_BROWSER_AUTO_RESPONSE_ENABLED=true
HH_BROWSER_AUTO_RESPONSE_MODE=PREFLIGHT
HH_BROWSER_AUTO_RESPONSE_HEADLESS=false
HH_BROWSER_USER_DATA_DIR=C:\Users\beats\.jobhunterpro\hh-chrome-profile
HH_BROWSER_WAIT_TIMEOUT_SECONDS=10
```

Для реального отклика:

```text
HH_BROWSER_AUTO_RESPONSE_MODE=EXECUTE
```

Нельзя включать в отчёт, Git, screenshots и логи:

```text
OPENROUTER_API_KEY
HH_CLIENT_SECRET
HH access_token
HH refresh_token
OAuth authorization code
cookies / browser session data
```

В старом `application.yaml` ранее уже могли быть показаны реальные секреты. Их нужно считать скомпрометированными, перевыпустить и вынести в environment variables.

---

## 9. Primary resume pipeline

### 9.1. Модель и миграции

В БД существует таблица `resumes`.

Добавлена миграция:

```text
V4__add_primary_resume_flag.sql
```

Содержимое по смыслу:

```text
- resumes.is_primary BOOLEAN NOT NULL DEFAULT FALSE;
- для каждого user назначается одно наиболее актуальное primary resume;
- partial unique index:
  uq_resumes_primary_per_user
  ON resumes(user_id)
  WHERE is_primary = TRUE.
```

В последнем запуске Flyway показывал:

```text
Validated 5 migrations
Current schema version: 5
```

Не угадывать содержимое V5 без актуального файла.

### 9.2. Core resume classes

```text
ResumeId
Resume
ResumeSourceType
ResumeRepository
ResumeDto
PrimaryResumeContentDto
GetPrimaryResumeUseCase
GetPrimaryResumeContentUseCase
SavePrimaryResumeUseCase
UploadPrimaryResumePdfUseCase
UploadPrimaryResumePdfCommand
PdfTextExtractionPort
```

### 9.3. Infrastructure resume classes

```text
ResumeEntity
SpringDataResumeJpaRepository
ResumePersistenceMapper
ResumeRepositoryAdapter
ResumeService
PdfBoxPdfTextExtractionAdapter
PdfResumeUploadService
```

### 9.4. Важные детали

```text
- ResumeRepositoryAdapter НЕ должен быть final:
  он использует TransactionTemplate / Spring proxy.
- PDF max size: 15 MB.
- Сканированные PDF без извлекаемого текста отклоняются.
- Текст PDF ограничивается защитным лимитом.
- Upload primary resume работает из JavaFX AutoResponses UI.
```

---

## 10. UI загрузки primary PDF resume

`AutoResponsesController` умеет:

```text
- показывать статус primary resume;
- открыть FileChooser;
- выбрать PDF;
- прочитать bytes;
- вызвать UploadPrimaryResumePdfUseCase;
- обновить статус.
```

В FXML добавлен блок:

```text
Резюме для автооткликов: <статус>
[Загрузить PDF]
```

Важное поведение:

```text
Перед запуском автоотклика проверяется наличие primary resume.
```

---

## 11. HH vacancy details для LLM

Добавлены:

```text
HhVacancyDetailsDto
GetHhVacancyDetailsUseCase
HhVacancyDetailsResponse
HhVacancyDetailsService
HhApiClient.getVacancyDetailsAuthorized(...)
```

Особенности:

```text
- Детали вакансии загружаются через HH application token.
- HTML description очищается через Jsoup.parseBodyFragment(...).text().
- Короткого DTO из vacancy search недостаточно: он не содержит полноценное описание.
```

---

## 12. Генерация сопроводительного письма

### 12.1. Классы

```text
GenerateCoverLetterCommand
GeneratedCoverLetterDto
GenerateCoverLetterUseCase
GenerateCoverLetterService
CoverLetterQualityValidator
GeneratedCoverLetterQualityException
```

### 12.2. Pipeline

```text
queue item READY
→ get HH vacancy details
→ get primary resume content
→ GenerateCoverLetterUseCase
→ quality gate
→ HhBrowserAutoResponseAgent.apply(...)
```

### 12.3. Prompt restrictions

Сопроводительное письмо:

```text
- на русском;
- без Markdown, HTML, emoji;
- без выдуманного опыта;
- связано с вакансией и резюме;
- краткое, деловое, готово для вставки на HH;
- 2–4 абзаца;
- без темы письма;
- без вымышленных контактов.
```

### 12.4. Quality gate

Введена защита качества:

```text
- минимум 250 символов;
- максимум 4000 символов;
- минимум 2 предложения;
- очистка whitespace;
- невалидный текст не должен уходить в HH.
```

Это важно, потому что `openrouter/free` в одном из ранних запусков вернул письмо длиной около 17 символов.

---

## 13. HhAutoResponseExecutionAdapter

Ключевые зависимости:

```text
HhBrowserAutoResponseAgent
HhBrowserAutoResponseProperties
GetHhVacancyDetailsUseCase
GetPrimaryResumeContentUseCase
GenerateCoverLetterUseCase
applicationTaskExecutor
```

Flow:

```text
prepareAutoResponse:
- параллельно получить detailed vacancy и primary resume;
- сгенерировать cover letter;
- quality gate;
- подготовить PreparedHhAutoResponse(vacancy, resumeText, coverLetter).

executeBrowserAutoResponse:
- сформировать HhQuestionnaireGenerationContext;
- вызвать browserAgent.apply(..., executionId);
- преобразовать outcome в AutoResponseExecutionResultDto.
```

### 13.1. Косметический долг

В некоторых текущих логах всё ещё написано:

```text
dryRun=EXECUTE
```

Хотя уже используется `mode=EXECUTE/PREFLIGHT`.

Это не влияет на логику, но нужно позже переименовать строку логирования на:

```text
mode={}
```

---

## 14. Режимы browser-agent

Вместо старого неявного `dryRun` введён явный режим:

```java
HhBrowserAutoResponseMode {
    PREFLIGHT,
    EXECUTE
}
```

### 14.1. PREFLIGHT

```text
- Получает подробности вакансии.
- Загружает primary resume.
- Генерирует и валидирует cover letter.
- Открывает страницу HH через Selenium.
- Проверяет авторизацию.
- Проверяет наличие активной кнопки «Откликнуться».
- Не кликает по кнопке.
- Сохраняет диагностику.
- Возвращает PREFLIGHT_VERIFIED / PREFLIGHT_COMPLETED.
- Очередь остаётся READY.
```

### 14.2. EXECUTE

```text
- Выполняет те же подготовительные проверки.
- В той же Selenium-сессии делает реальный отклик.
- Обрабатывает форму сопроводительного письма.
- Обрабатывает post-response форму письма.
- Может остановиться на анкете работодателя в review mode.
```

---

## 15. Diagnostics, executionId и browser sessionId

Добавлены:

```text
executionId = UUID на один запуск pipeline.
browserSessionId = Selenium RemoteWebDriver sessionId.
```

Логи содержат:

```text
HH browser run started:
executionId=...
sessionId=...
vacancyUrl=...

HH browser diagnostics saved:
executionId=...
sessionId=...
reason=...
htmlPath=...

HH browser run finished:
executionId=...
sessionId=...
```

Диагностика сохраняется в:

```text
logs/hh-browser-debug/<executionId>/
```

Каждый запуск должен иметь отдельную папку.

Важно:

```text
Разные папки — это разные executionId / разные запуски.
Это не означает, что один запуск случайно открыл браузер дважды.
```

`saveDiagnosticsOnce(...)` предотвращает несколько HTML/PNG наборов внутри одного browser run.

---

## 16. Selenium/Chrome состояние

### 16.1. Persistent profile

Используется отдельный профиль:

```text
C:\Users\beats\.jobhunterpro\hh-chrome-profile
```

В нём пользователь вручную авторизован на HH.ru.

### 16.2. Driver config

В `HhBrowserDriverFactory` важны:

```text
--user-data-dir=<profile>
--profile-directory=Default
--start-maximized
--disable-notifications
PageLoadStrategy.EAGER
pageLoadTimeout(properties.waitTimeout())
```

`PageLoadStrategy.EAGER` был нужен, чтобы `driver.get(...)` не зависал на долгой полной загрузке HH.

### 16.3. CDP warning

Лог может содержать:

```text
Unable to find an exact match for CDP version 149,
returning closest version 148.
```

Это предупреждение, не блокировавшее успешные сценарии Selenium.

---

## 17. Успешный реальный автоотклик

Подтверждён полный flow:

```text
HH API detailed vacancy
→ PDF resume
→ OpenRouter cover letter
→ cover letter quality gate
→ Selenium
→ «Откликнуться»
→ HH сразу отправил resume
→ post-response форма сопроводительного письма
→ textarea[name='text']
→ [data-qa='vacancy-response-letter-submit']
→ подтверждение отправки
→ RESPONSE_SENT_WITH_COVER_LETTER
→ SUCCESS
→ queue item SENT.
```

### 17.1. Важное исправление post-response формы

HH на некоторых вакансиях отправляет резюме сразу после первого клика. После этого появляется отдельный блок:

```html
<div data-qa="vacancy-response-letter-informer">
<textarea name="text"></textarea>
<button data-qa="vacancy-response-letter-submit">Отправить</button>
```

Ранее код возвращал `RESPONSE_SENT_WITHOUT_COVER_LETTER` слишком рано.

Теперь у него есть логика:

```text
trySendPostResponseCoverLetter(...)
```

Если форма найдена, письмо вставляется и отправляется. Если формы действительно нет — возвращается partial success.

---

## 18. Questionnaire / вопросы работодателя

### 18.1. Начальная цель

Подключить LLM для:

```text
- извлечения вопросов работодателя;
- генерации ответов;
- заполнения текстовых полей Selenium;
- сохранения HTML/PNG;
- review до финальной отправки.
```

### 18.2. Реализованные DTO и use case

В core добавлены:

```text
HhQuestionnaireQuestionDto
GeneratedHhQuestionnaireAnswerDto
GenerateHhQuestionnaireAnswersCommand
GeneratedHhQuestionnaireAnswersDto
GenerateHhQuestionnaireAnswersUseCase
```

В infrastructure:

```text
GenerateHhQuestionnaireAnswersService
HhQuestionnaireGenerationContext
```

### 18.3. Генерация структурированных ответов

Use case:

```text
answer-hh-questionnaire
```

Ожидаемый LLM JSON:

```json
{
  "answers": [
    {
      "fieldName": "task_123_text",
      "answer": "Текст ответа"
    }
  ]
}
```

Сервис валидирует:

```text
- JSON существует;
- answers не пуст;
- нет неизвестных fieldName;
- нет дублей;
- есть ответ на каждый ожидаемый вопрос;
- answer не blank;
- answer не длиннее 1200 символов;
- итоговый порядок соответствует DOM-порядку вопросов.
```

---

## 19. Поддерживаемые типы вопросов HH

### 19.1. Текстовые вопросы — поддерживаются в режиме review

DOM-ориентиры:

```text
[data-qa='task-body']
[data-qa='task-question']
textarea[name^='task_'][name$='_text']
```

Имя поля читается так:

```java
textarea.getDomAttribute("name")
```

Важно:

```text
Не использовать getDomProperty("value") для чтения name:
value — это введённый текст.
name — это идентификатор поля формы.
```

Заполненное значение после `sendKeys(...)` проверяется через:

```java
textarea.getDomProperty("value")
```

### 19.2. Checkbox / radio / select — пока не поддерживаются автоматически

Примеры:

```text
часовой пояс;
готовность к переезду;
формат работы;
уровень английского;
выбор из предопределённых вариантов.
```

Причина:

```text
Такие ответы содержат личные факты кандидата.
LLM не должна выдумывать часовой пояс, зарплату,
готовность к переезду, личные предпочтения и т.п.
```

При обнаружении choice fields текущий предполагаемый безопасный результат:

```text
QUESTIONNAIRE_REQUIRED
reason=questionnaire-choice-fields-review-required
```

Автовыбор вариантов ещё не реализован.

---

## 20. Исправления questionnaire detection

Ранее `hasQuestionForm(...)` искал текстовые фразы по всей странице, что давало ложные срабатывания.

Исправленный принцип:

```java
private boolean hasQuestionForm(WebDriver driver) {
    return !driver.findElements(
            QUESTIONNAIRE_TASK_BODY_SELECTOR
    ).isEmpty();
}
```

После отправки cover letter агент ждёт один из двух сценариев:

```text
1. Появилась анкета:
   → fillQuestionnaireForReview(...)

2. Появилось подтверждение отправки:
   → RESPONSE_SENT_WITH_COVER_LETTER.
```

Ключевой метод:

```text
waitForStateAfterCoverLetterSubmit(...)
```

---

## 21. Подтверждённый результат текстовой анкеты

Последний успешный proof-of-concept анкеты:

```text
- LLM use case answer-hh-questionnaire вызван.
- OpenRouter вернул JSON.
- Selenium заполнил текстовые вопросы.
- Заполнены ответы на вопросы про:
  - желаемую зарплату;
  - уровень Java;
  - Selenium/Selenide;
  - Cucumber/BDD.
- Сохранена диагностика:
  questionnaire-filled-review-required.
- Финальная кнопка анкеты намеренно НЕ нажата.
```

Итоговый статус:

```text
QUESTIONNAIRE_FILLED_REVIEW_REQUIRED
```

Поэтому пользователь не увидел отправленный отклик — это ожидаемое поведение текущего review-режима, а не ошибка Selenium.

---

## 22. Текущая проблема качества ответов анкеты

LLM ответила на часть вопросов технической фразой:

```text
«Информация об опыте работы с Selenium и Selenide требует уточнения кандидатом».
«Информация об опыте работы с Cucumber (BDD) требует уточнения кандидатом».
```

Это произошло потому, что в system prompt было правило:

```text
Если достоверного ответа нет,
напиши коротко и честно,
что информация требует уточнения кандидатом.
```

Для внутренней диагностики это безопасно, но для настоящей анкеты работодателя — плохой UX. Такие фразы не должны автоматически уходить работодателю.

### Текущий вывод

```text
Проблема не в извлечении вопросов и не в Selenium.
Проблема в отсутствии профиля фактов кандидата и в policy для неизвестных данных.
```

---

## 23. Текущая точка остановки

Остановились после успешного режима:

```text
QUESTIONNAIRE_FILLED_REVIEW_REQUIRED
```

Технически сейчас уже работает:

```text
text questionnaire extraction
→ structured JSON LLM answer generation
→ validation
→ Selenium fill
→ screenshot + HTML diagnostics
→ no final submit.
```

Но перед автоматической отправкой анкет нужно улучшить два направления:

```text
1. Candidate Questionnaire Profile:
   хранить реальные факты, которые нельзя достоверно вывести из PDF:
   - часовой пояс;
   - минимальные/целевые зарплатные ожидания;
   - опыт Selenium;
   - опыт Selenide;
   - опыт Cucumber / BDD;
   - готовность к переезду;
   - желаемый формат работы;
   - английский;
   - готовность к командировкам;
   - дата готовности начать работу;
   - другие повторяемые facts.

2. Answer quality policy:
   - не вставлять «требует уточнения кандидатом» в реальные поля;
   - неизвестный важный факт должен переводить вопрос в review/manual-required;
   - не отправлять анкету автоматически, если хотя бы один required answer unresolved.
```

---

## 24. Рекомендуемый следующий этап

### 24.1. Название

```text
2.6 — Candidate Questionnaire Profile and Answer Quality Gate
```

### 24.2. Цель

Сделать так, чтобы LLM отвечала на вопросы работодателя только на основе:

```text
- primary resume;
- подробностей вакансии;
- сохранённого profile facts кандидата.
```

И чтобы система могла различать:

```text
READY_TO_FILL:
ответ подтверждён фактами и может быть вставлен.

REVIEW_REQUIRED:
ответ требует решения пользователя, но не является технической ошибкой.

UNSUPPORTED:
тип поля или вопрос пока не поддерживаются.
```

### 24.3. Рекомендуемая модель

В core предложить:

```text
CandidateQuestionnaireProfile
CandidateQuestionnaireProfileRepository
GetCandidateQuestionnaireProfileUseCase
SaveCandidateQuestionnaireProfileUseCase
```

Пример данных:

```java
public record CandidateQuestionnaireProfile(
        UserId userId,
        String timezone,
        Integer salaryExpectationFrom,
        Integer salaryExpectationTo,
        String salaryCurrency,
        String seleniumExperience,
        String selenideExperience,
        String cucumberBddExperience,
        String englishLevel,
        Boolean relocationReady,
        String workFormatPreference,
        String startAvailability,
        String additionalFacts
) {}
```

Не обязательно использовать именно эти поля — сначала проверить текущую user/profile модель и UI.

### 24.4. Изменить результат генерации

Вместо только `answer` полезно добавить статус:

```text
QuestionnaireAnswerResolution:
- RESOLVED
- REVIEW_REQUIRED
```

И DTO:

```java
public record GeneratedHhQuestionnaireAnswerDto(
        String fieldName,
        String answer,
        QuestionnaireAnswerResolution resolution,
        String reviewReason
) {}
```

### 24.5. Изменить prompt

Вместо технической фразы:

```text
«требует уточнения кандидатом»
```

нужно вернуть structured review marker, например:

```json
{
  "fieldName": "task_123_text",
  "answer": null,
  "resolution": "REVIEW_REQUIRED",
  "reviewReason": "Нет подтверждённых данных об опыте Selenium/Selenide"
}
```

Тогда Selenium:

```text
- не вставляет заглушку;
- сохраняет diagnostics;
- возвращает questionnaire review required;
- не отправляет анкету.
```

---

## 25. Режимы работы анкеты

Сейчас по факту реализован только безопасный режим:

```text
REVIEW:
- заполнить только подтверждённые текстовые ответы;
- не нажимать final submit;
- вернуть QUESTIONNAIRE_FILLED_REVIEW_REQUIRED.
```

Позже полезно явно ввести:

```text
QUESTIONNAIRE_MODE=REVIEW | SUBMIT
```

### REVIEW

```text
- безопасный режим разработки;
- показывает ответы в Chrome;
- сохраняет HTML/PNG;
- не отправляет форму.
```

### SUBMIT

```text
- возможен только после quality gate;
- только если все required ответы RESOLVED;
- перед submit желательно отдельное подтверждение/настройка;
- после click проверять HH success state.
```

Пока `SUBMIT` для анкеты не реализовывать без явного решения пользователя.

---

## 26. Что не нужно делать в следующем чате

```text
- Не включать финальную отправку questionnaire автоматически.
- Не использовать LLM для выбора checkbox/radio/select без profile facts.
- Не превращать основной workflow в ручной вместо автооткликов.
- Не смешивать в одном патче:
  profile persistence,
  UI,
  prompt redesign,
  browser submit.
  Лучше идти небольшими проверяемыми этапами.
- Не использовать/не выводить реальные API tokens и secrets.
```

---

## 27. Какие актуальные файлы попросить перед продолжением

Перед реализацией Candidate Questionnaire Profile попросить актуальные версии:

```text
User.java / UserId.java
UserEntity.java
UserRepository.java
UserRepositoryAdapter.java
UserService.java
ProfileController.java
profile.fxml
application.yaml
V1__init_schema.sql
все последующие Flyway migrations
HhBrowserAutoResponseAgent.java
HhAutoResponseExecutionAdapter.java
GenerateHhQuestionnaireAnswersService.java
GenerateHhQuestionnaireAnswersCommand.java
GeneratedHhQuestionnaireAnswerDto.java
GeneratedHhQuestionnaireAnswersDto.java
HhQuestionnaireGenerationContext.java
AutoResponseExecutionStatus.java
AutoResponseExecutionResultDto.java
AutoResponseExecutionService.java
```

Также попросить пользователя дать точные реальные ответы/предпочтения для профиля:

```text
1. Часовой пояс.
2. Зарплатные ожидания: минимум / желаемая сумма / валюта.
3. Реальный опыт Selenium.
4. Реальный опыт Selenide.
5. Реальный опыт Cucumber / BDD.
6. Готовность к переезду.
7. Предпочтительный формат работы.
8. Английский.
9. Готовность к командировкам.
10. Когда готов начать работу.
```

---

## 28. Команды проверки

### Полный тест infrastructure + dependencies

```powershell
mvn -pl infrastructure -am test
```

### Общий набор тестов

```powershell
mvn clean test
```

### Проверка остатков старого dryRun naming

```powershell
Get-ChildItem -Recurse -Filter *.java |
    Select-String -Pattern "dryRun\(|DRY_RUN_VERIFIED|DRY_RUN_COMPLETED|dry-run"
```

### Debug artifacts

```text
logs/hh-browser-debug/<executionId>/
```

---

## 29. Известные технические замечания

```text
- Selenium может писать warning о CDP 149 / closest 148.
  В текущих подтверждённых сценариях это не ломало выполнение.

- `openrouter/free` нестабилен:
  может вернуть пустой content, плохой JSON или слабый ответ.
  Retry/fallback strategy была сознательно отложена пользователем.
  Не переключаться на неё сейчас, пока не закрыт Questionnaire Profile.

- Генерация letters/answers может занимать десятки секунд.
  Это ожидаемо для free OpenRouter routing.

- В текущих логах может остаться подпись dryRun=EXECUTE.
  Это косметический technical debt, не логическая проблема.

- Queue status при QUESTIONNAIRE_FILLED_REVIEW_REQUIRED должен оставаться READY.
  Не переводить его в SENT и не считать FAILED.
```

---

## 30. Краткий prompt для следующего чата

```text
Продолжаем JobHunterPro Desktop Application.

Прочитай приложенный отчёт и опирайся на него. Не угадывай отсутствующие файлы — проси актуальные версии.

Текущая точка:
- HH browser auto-response с LLM cover letter работает end-to-end.
- Для текстовых анкет HH.ru LLM уже генерирует JSON-ответы, Selenium вставляет их в поля, но финальный submit намеренно не нажимается.
- Получен статус QUESTIONNAIRE_FILLED_REVIEW_REQUIRED.
- Нужно перейти к Candidate Questionnaire Profile и quality gate: хранить реальные факты кандидата и не вставлять в форму фразы вида «требует уточнения кандидатом».

Сначала запроси актуальные файлы User/Profile, migrations, HhBrowserAutoResponseAgent и GenerateHhQuestionnaireAnswersService.
После этого предложи минимальную поэтапную реализацию:
1. domain + persistence profile facts;
2. UI редактирования профиля;
3. structured review/resolved answers;
4. только потом optional questionnaire submit mode.
```

---

## 31. Самый короткий итог

```text
Что работает:
- HH OAuth и vacancy API search;
- primary PDF resume;
- OpenRouter LLM;
- AI cover letters;
- real Selenium HH auto response;
- post-response cover letter;
- PREFLIGHT diagnostics;
- text questionnaire extraction;
- JSON LLM answers;
- Selenium fill in review mode.

Где остановились:
- ответы анкеты уже вставляются;
- не все ответы качественные без профиля фактов кандидата;
- анкета намеренно не отправляется.

Следующий этап:
Candidate Questionnaire Profile + Answer Quality Gate.
```
