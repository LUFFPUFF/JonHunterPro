# JobHunterPro Desktop Application - полный отчёт для перехода в новый чат

**Дата составления:** 2026-06-27  
**Проект:** `JobHunterPro Desktop Application`  
**Локальный путь проекта:** `C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application`  
**Назначение документа:** передать следующему чату актуальный контекст разработки: от ранее реализованного HH OAuth и LLM-first обработки анкет до массового batch-запуска автооткликов и результатов первого реального прогона на 47 вакансиях.

> **Ключевое правило для следующего чата:** сначала изучить `JobHunterPro_Master_Prompt`, затем этот отчёт, затем актуальные исходники и последний runtime-лог. Не строить патчи по памяти, старым версиям классов или предположениям. Истина о текущей реализации - фактические исходники и runtime-артефакты, приложенные в новом чате.

---

## 1. Как использовать документы и порядок приоритета

В новом чате должны быть приложены минимум:

1. `JobHunterPro_Master_Prompt(8).md` - обязательная базовая спецификация качества, архитектурных принципов и процесса работы.
2. Этот отчёт - актуальный журнал решений, текущего статуса и ограничений.
3. Последний архив исходников после batch-реализации.
4. Последний runtime-лог batch-прогона и browser diagnostics по проблемным вакансиям.

Порядок применения правил:

```text
1. JobHunterPro_Master_Prompt - обязательный стандарт архитектуры, качества кода,
   тестов, DI, асинхронности и поэтапной разработки.
2. Этот отчёт - актуальное состояние проекта и подтверждённые продуктовые решения.
3. Реальные исходники и runtime-логи - единственный источник правды о том,
   что действительно реализовано и работает сейчас.
```

Если Master Prompt расходится с этим отчётом по устаревшим историческим решениям, действуют **актуальные подтверждённые решения отчёта и кода**. Например: в проекте используется PostgreSQL на `localhost:5434`, а не SQLite; `qwen3:14b` должна оставаться primary LLM, а OpenRouter - fallback.

---

## 2. Продуктовая цель и ограничения

`JobHunterPro` - desktop-приложение для реальных автооткликов на вакансии. Главная ценность продукта - не ручное открытие вакансии пользователем, а автоматический управляемый поток:

```text
Поиск вакансий
→ добавление в очередь
→ подготовка сопроводительного письма и ответов анкеты
→ Selenium browser automation в авторизованном HH-профиле
→ техническая проверка результата
→ итоговый статус в очереди.
```

Нельзя потерять следующие решения:

```text
- Основная функция продукта - настоящие автоотклики.
- Ручная проверка допустима только как safety fallback, а не как главный сценарий.
- Активная интеграция: HH.ru.
- Habr Career пока отложен из-за внешних ограничений OAuth/API.
- LLM не должна выдумывать проверяемые факты: стаж, работодателей,
  образование, GPA, технологии, проекты, документы, личные обстоятельства.
- Предпочтения кандидата могут использоваться как входные факты:
  зарплата, формат работы, офис/удалёнка, переезд, командировки,
  тестовое задание, дата выхода и подобные параметры.
- Массовая обработка должна быть последовательной: один browser-worker,
  без запуска десятков Chrome/Selenium-сессий одновременно.
```

---

## 3. Архитектура и окружение

### 3.1. Модули

```text
core/
- domain-модели, DTO, use cases, ports;
- без Spring, Selenium, JavaFX, JPA и HTTP-клиентов.

infrastructure/
- persistence, Flyway/JPA, LLM providers, Jinjava prompts,
  Selenium/HH browser agent, HTTP clients, adapter-реализации портов.

ui/
- JavaFX controllers и FXML.

app/
- Spring Boot bootstrap и конфигурация.
```

### 3.2. Технологии

```text
Java 21
JavaFX 21
Spring Boot 3.5.x
Maven multi-module
PostgreSQL 16 на localhost:5434
Flyway + Hibernate/JPA
Selenium WebDriver + Chrome user profile
OkHttp 5
Jackson
Jinjava 2.8.0
Ollama
OpenRouter
JUnit 5 + Mockito
PDFBox + Jsoup
```

### 3.3. Важные локальные настройки

```text
Windows user: beats
Project: C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application
PostgreSQL: localhost:5434/jobhunter
HH OAuth redirect URI: jobhunterpro://oauth/hh/callback
Custom scheme callback forward port: 54347
Chrome profile: %USERPROFILE%\.jobhunterpro\hh-chrome-profile
```

---

## 4. Что было реализовано до batch-этапа

### 4.1. HH OAuth и поиск

Подтверждённо работали:

```text
- OAuth HH.ru через jobhunterpro://oauth/hh/callback;
- сохранение токена в external_auth_tokens;
- HH connection status = CONNECTED;
- проверка текущего пользователя через HH API;
- поиск вакансий HH;
- переключение страниц результатов и размер страницы 20 / 50 / 100;
- корректная ссылка поиска на voronezh.hh.ru с нужными параметрами.
```

### 4.2. Очередь автооткликов

До текущего этапа были реализованы:

```text
- добавление вакансии в очередь с защитой от дублей по user + source + externalVacancyId;
- просмотр полной очереди и READY-вакансий;
- одиночная смена статуса;
- загрузка primary resume;
- browser auto-response с Chrome-профилем, где пользователь авторизован в HH;
- сохранение diagnostics HTML/PNG/JSON.
```

### 4.3. LLM и сопроводительные письма

Принятое продуктовое направление:

```text
Ollama qwen3:14b - primary provider.
OpenRouter - fallback provider.
```

Важное правило: `qwen3:14b` должна оставаться primary в целевой архитектуре. Однако в последнем массовом запуске Ollama аварийно завершалась на каждом запросе, поэтому фактический runtime уходил в OpenRouter fallback.

### 4.4. LLM-first анкеты HH.ru

Целевой pipeline анкет:

```text
HH DOM
→ извлечение единой схемы формы
→ один LLM-запрос на всю форму
→ структурированный ответ
→ Java schema/DOM validation
→ Selenium fill
→ проверка фактического DOM
→ submit.
```

Распределение ответственности:

```text
LLM:
- понимает смысл всей анкеты;
- выбирает radio-варианты;
- формирует textarea;
- решает использование «Свой вариант»;
- использует резюме, профиль и вакансию как контекст.

Java/Selenium:
- извлекают реальные поля и варианты из DOM;
- валидируют технический контракт;
- сопоставляют индексы с реальными DOM fields;
- заполняют форму;
- проверяют DOM;
- не отправляют неполную/невалидную форму.
```

Не возвращать `HhQuestionnaireAnswerGroundingGate` в блокирующий execution-path. Его можно использовать только как audit/risk diagnostics, но не как механизм, отменяющий смысловое решение LLM.

---

## 5. Последовательность реализованных подэтапов текущей сессии

### 5.1. Candidate approval и diagnostics metadata

Цель: не терять информацию о том, почему анкета остановилась и требует пользователя.

Реализованный поток:

```text
HhBrowserAutoResponseAgent
→ HhBrowserAutoResponseResult
→ HhAutoResponseExecutionAdapter
→ AutoResponseExecutionResultDto
→ AutoResponseExecutionService
→ queue persistence / UI.
```

Ключевые изменения:

```text
- HhBrowserAutoResponseResult возвращает outcome, candidateApprovalReason,
  diagnosticDirectory.
- Новый execution status: CANDIDATE_APPROVAL_REQUIRED.
- Новый queue status: WAITING_CANDIDATE_APPROVAL.
- В queue item сохраняются candidateApprovalReason и diagnosticDirectory.
- Flyway migration V10 добавляет candidate_approval_reason и diagnostic_directory.
- Browser diagnostics группируются по vacancy ID:
  logs/hh-browser-debug/<externalVacancyId>/<executionId>/.
- UI показывает ожидание кандидата, ID HH, ссылку на вакансию,
  причину и кнопку открытия diagnostics.
```

Важно: `DIAGNOSTIC_ONLY` и технические ограничения не должны автоматически считаться ожиданием ответа кандидата. В `WAITING_CANDIDATE_APPROVAL` должны попадать только случаи, где реально нужен подтверждённый кандидатский факт.

Подтверждённо: Maven build после внесения изменений проходил успешно.

### 5.2. Массовая пометка вакансий как READY

Реализован безопасный batch-переход:

```text
QUEUED → READY          можно массово
READY → READY           «уже готова»
WAITING... / SENT /
FAILED / SKIPPED        не меняются массово
```

Добавлено:

```text
- MarkAutoResponseQueueItemsReadyCommand;
- MarkAutoResponseQueueItemsReadyResultDto;
- MarkAutoResponseQueueItemsReadyUseCase;
- batch-обработка в AutoResponseQueueService;
- unit-test AutoResponseQueueServiceBatchReadyTest;
- чекбоксы для QUEUED в UI;
- select-all только по текущим QUEUED;
- итоговая статистика batch-обновления.
```

Подтверждённо: Maven build и UI-проверка прошли успешно.

### 5.3. Атомарный захват READY-вакансии перед запуском

Для предотвращения двойного запуска реализована стадия:

```text
READY → IN_PROGRESS → финальный статус.
```

Ключевые элементы:

```text
- AutoResponseQueueStatus.IN_PROGRESS;
- AutoResponseQueueRepository.claimReadyForExecution(...);
- атомарный JPQL update в SpringData repository:
  захватить можно только статус READY;
- AutoResponseExecutionService сначала claim, потом вызывает browser flow;
- при временной LLM-проблеме элемент возвращается в READY;
- UI блокирует ручные действия для IN_PROGRESS.
```

Подтверждённо: Maven build прошёл успешно.

### 5.4. Backend batch-runner

Добавлен worker для всех текущих READY-вакансий:

```text
StartReadyAutoResponsesBatchUseCase
→ ReadyAutoResponseBatchService
→ single virtual-thread executor
→ последовательный ExecuteAutoResponseUseCase для каждого item.
```

Основные классы:

```text
AutoResponseBatchStartStatus
AutoResponseBatchProgressStatus
StartReadyAutoResponsesBatchCommand
StartReadyAutoResponsesBatchResultDto
AutoResponseBatchProgressDto
StartReadyAutoResponsesBatchUseCase
GetAutoResponseBatchProgressUseCase
AutoResponseBatchExecutionConfiguration
AutoResponseBatchProgressStore
ReadyAutoResponseBatchService
ReadyAutoResponseBatchServiceTest
```

Свойства реализации:

```text
- один active batch на desktop-приложение;
- один virtual-thread worker;
- последовательное выполнение Selenium;
- снимок READY элементов на момент запуска;
- текущий progress хранится в памяти;
- повторное нажатие возвращает ALREADY_RUNNING;
- item, который успел перестать быть READY, считается skipped;
- итог различает sent / candidate approval / returned to READY / failed / skipped.
```

Подтверждённо: Maven build прошёл успешно.

### 5.5. UI для массового запуска и live-progress

Добавлено:

```text
- кнопка «Запустить все готовые»;
- предварительная проверка primary resume;
- блокировка ручных действий и batch выбора на время активного запуска;
- Timeline с polling progress раз в секунду;
- обновление очереди при изменении числа started/processed;
- итоговый текст с числом отправленных, ожидающих кандидата,
  возвращённых в READY, failed и skipped.
```

Подтверждённо: Maven build прошёл успешно и был запущен реальный batch.

---

## 6. Реальный batch-запуск 2026-06-27

### 6.1. Общий результат

Фактический запуск:

```text
Batch ID: dcab9b8f-7878-467b-9f59-f52acf65ec3a
План: 47 вакансий
Запущено: 47
Длительность: около 47 минут 42 секунд
Итог: COMPLETED_WITH_ISSUES
```

Итоговая статистика `AutoResponseBatchProgressDto`:

| Категория | Количество | Что это означает |
|---|---:|---|
| `SUCCESS` | 1 | отклик отправлен с подтверждённым cover letter |
| `PARTIAL_SUCCESS` | 12 | отклик отправлен, но agent не нашёл/не подтвердил форму сопроводительного письма |
| `NOT_AVAILABLE` | 13 | LLM не подготовила пригодное письмо; элемент возвращён в `READY` |
| `QUESTIONNAIRE_REQUIRED` | 2 | анкета не была обработана из-за LLM/diagnostic режима; элемент возвращён в `READY` |
| `CANDIDATE_APPROVAL_REQUIRED` | 2 | реально нужен ответ кандидата; элемент переведён в `WAITING_CANDIDATE_APPROVAL` |
| `FAILED` | 17 | подготовка отклика упала; в логе главным источником являются 429 OpenRouter |
| `SKIPPED` | 0 | не было вакансий, потерявших READY до захвата |

Ключевой вывод: **из 47 вакансий только один отклик подтверждён как отправленный с сопроводительным письмом**. Ещё 12 ушли с `PARTIAL_SUCCESS`, то есть результат нельзя считать качественным успехом для цели персонализированных автооткликов.

### 6.2. Вакансии по итоговому статусу

```text
SUCCESS (1):
131318315

PARTIAL_SUCCESS / response-sent-without-cover-letter-form (12):
134571910, 134573930, 134429843, 134568396, 134580849, 134574533,
134578939, 134260067, 132203076, 133905802, 134530818, 129167274

NOT_AVAILABLE / returned to READY (13):
134525533, 134570214, 134586770, 132109413, 134090118, 134595464,
134606035, 108265661, 131602878, 134581498, 134611943, 134241414,
134606644

QUESTIONNAIRE_REQUIRED / returned to READY (2):
133656620, 133906072

CANDIDATE_APPROVAL_REQUIRED / WAITING_CANDIDATE_APPROVAL (2):
130279256, 133918651

FAILED (17):
134620074, 131768208, 133756504, 134572528, 133658895, 134597280,
134600503, 134602117, 134610127, 134595088, 133960898, 134607712,
133401154, 134515837, 134478662, 133973542, 134451347
```

### 6.3. Подтверждённые runtime-факты из `logs.txt`

#### Ollama primary не работала в этом batch

Повторяющаяся ошибка:

```text
Ollama request failed with HTTP status 500
llama-server process has terminated
CUDA error: the provided PTX was compiled with an unsupported toolchain
```

Следствие: на этом запуске `qwen3:14b` формально оставалась primary-route, но фактически не сгенерировала письма. Запросы постоянно уходили в OpenRouter fallback.

#### OpenRouter упёрся в лимиты free tier

В логе присутствуют повторные ошибки:

```text
HTTP 429
Rate limit exceeded: free-models-per-day
```

В текущей модели такие provider failures могут попадать в `FAILED`. Это неверная бизнес-семантика: вакансия не стала невалидной, временно недоступен генератор текста.

#### Пустой output при `finishReason=length`

Наблюдались ответы OpenRouter с:

```text
finishReason=length
contentLength=0
completionTokens=420
```

Это означает, что лимит генерации был израсходован, но пригодное письмо не получено. Browser flow должен вообще не начинаться для такого результата, а задача должна переходить в retryable состояние.

#### 12 откликов отправлены без подтверждённой формы письма

У 12 диагностик причина:

```text
response-sent-without-cover-letter-form
```

В приложенном логе есть пути к HTML/PNG diagnostics, но сами HTML/PNG этих 12 случаев не были приложены в текущий архив. Поэтому нельзя честно утверждать, что поле письма действительно отсутствовало на HH: возможны два сценария.

```text
A. HH действительно не показал поле сопроводительного письма.
B. Selenium не нашёл, не дождался или не подтвердил поле из-за selector/timing/DOM transition.
```

Сначала нужно изучить минимум 3 сохранённых HTML/PNG из этой группы. До этого нельзя безопасно «лечить selectors» вслепую.

#### UI NPE после обновления очереди

После обновления списка наблюдался:

```text
NullPointerException:
Cannot invoke AutoResponseQueueItemDto.status() because selectedItem is null
at AutoResponsesController.updateQueueActionButtons(...)
```

Причина: selection listener может вызвать `updateQueueActionButtons(null)` после `clearSelection()`. Это UI-дефект, который не остановил batch, но должен быть исправлен отдельным маленьким подэтапом.

---

## 7. Главная диагностика: что работает и что не работает

### Работает

```text
- Массовый запуск как координационный механизм.
- Последовательное выполнение одной браузерной сессии за раз.
- READY → IN_PROGRESS atomic claim.
- Сохранение статусов после выполнения.
- Candidate approval flow и diagnostics path.
- UI progress и блокировка ручных действий во время batch.
- Реальная отправка откликов на HH.
- Сохранение browser diagnostics по vacancy ID и execution ID.
```

### Не работает достаточно надёжно

```text
- Primary Ollama runtime: падает из-за CUDA/PTX toolchain ошибки.
- OpenRouter free fallback: rate limit и пустые ответы.
- Текущая семантика FAILED: временная provider failure смешана с неисправимой ошибкой.
- Гарантия наличия cover letter до submit.
- Диагностика причины «поле письма отсутствует» против «Selenium не нашёл поле».
- UI null-safety в updateQueueActionButtons.
- Pipeline сейчас объединяет генерацию письма и browser apply в один execution flow.
```

---

## 8. Целевая стратегия улучшения

### Принцип

Нельзя улучшать всё сразу и нельзя начинать с переписывания Selenium selectors. Сначала требуется сделать LLM-route управляемой, типизированной и наблюдаемой. Затем - строгая гарантия заполнения письма. Только потом - более глубокий redesign pipeline.

### Фаза A. Стабилизация LLM providers и retry semantics

Цель: устранить ложные `FAILED`, перестать повторять заведомо падающий provider для каждой вакансии и получить чистый диагностический прогон.

Нужные изменения:

```text
1. Ввести typed LLM failure categories, например:
   - OLLAMA_RUNTIME_CRASH;
   - OLLAMA_TIMEOUT;
   - OPENROUTER_RATE_LIMIT;
   - OPENROUTER_EMPTY_CONTENT;
   - INVALID_MODEL_OUTPUT;
   - NETWORK_UNAVAILABLE.

2. Добавить provider circuit breaker на use case / provider:
   - Ollama CUDA/PTX crash → OPEN на 10-15 минут;
   - следующие вакансии не пытаются заново падать на Ollama;
   - OpenRouter 429 → cooldown до nextRetryAt;
   - не выполнять 2-3 дорогих retry на каждой из десятков вакансий.

3. Ввести retryable queue semantics:
   - provider unavailable / 429 / empty response → READY или отдельный RETRY_SCHEDULED;
   - сохранять retryAttempt, nextRetryAt, lastFailureCategory, lastFailureMessage;
   - только детерминированные неисправимые browser/business errors → FAILED.

4. Перед batch выполнить реальный generation smoke-check:
   - не только HTTP health;
   - generate short text with qwen3:14b;
   - non-empty content required;
   - если Ollama не готова, явно показать пользователю режим fallback/cooldown.
```

### Фаза B. Строгая политика cover letter

Цель: не отправлять «качественный» массовый отклик без подтверждения, что письмо было вставлено, если пользователь явно не разрешил иное.

Нужные изменения:

```text
1. Настраиваемая policy:
   STRICT_COVER_LETTER - default.

2. В strict режиме:
   field found
   → text inserted
   → DOM value verified
   → submit allowed.

   field absent/not confirmed
   → submit prohibited
   → result COVER_LETTER_FIELD_NOT_FOUND / COVER_LETTER_VALUE_NOT_VERIFIED
   → item remains retryable or enters review status.

3. В дальнейшем можно добавить opt-in setting:
   «Разрешать отклик без сопроводительного письма».
   По умолчанию выключено.

4. На каждую browser попытку сохранять cover-letter-dom-audit.json:
   pageUrl, responseButtonFound, dialogFound, fieldFound, selectorMatched,
   fieldVisible, fieldEnabled, valueLengthBefore, valueLengthAfter,
   expectedTextPrefix, textVerified, submitAllowed.
```

### Фаза C. Разделение «подготовить письмо» и «отправить отклик»

Текущее выполнение смешивает:

```text
LLM generation → HH browser → questionnaire → submit.
```

Целевой вариант:

```text
1. PREPARE_COVER_LETTER
   - получает vacancy data;
   - генерирует письмо;
   - валидирует пригодность;
   - сохраняет текст, provider, model, timestamp, length;
   - возвращает статус COVER_LETTER_READY или RETRY_SCHEDULED.

2. APPLY_PREPARED_RESPONSE
   - берёт только подготовленное письмо;
   - открывает HH;
   - ищет поле;
   - вставляет;
   - verifies DOM;
   - обрабатывает анкету;
   - submit только при выполненной policy.
```

Преимущества:

```text
- provider failures не загрязняют browser flow;
- можно отдельно измерять качество генерации и Selenium;
- можно повторно запускать apply без регенерации письма;
- можно оставить понятный audit trail;
- можно безопаснее batch-обрабатывать десятки вакансий.
```

### Фаза D. Улучшение OpenRouter route

Нужно проверить актуальные LLM generation options и response policy.

Направление:

```text
- для cover letter отключить reasoning, если маршрут/модель поддерживают;
- сделать компактный prompt;
- задать достаточный output budget;
- не считать finishReason=length абсолютной ошибкой, если content непустой
  и проходит проверку длины/качества;
- contentLength=0 всегда классифицировать как typed temporary provider failure;
- не отправлять browser request без валидного prepared letter.
```

### Фаза E. UI и observability

```text
1. Исправить null-check в updateQueueActionButtons(selectedItem).
2. Разделить видимые итоги batch:
   - отправлено с письмом;
   - отправлено без письма;
   - поле письма не найдено;
   - ожидает кандидата;
   - ожидает повтор LLM;
   - browser/platform failure.
3. Добавить фильтрацию и повтор только retryable элементов.
4. Показать lastFailureCategory и nextRetryAt в очереди.
```

---

## 9. Рекомендуемый порядок следующих подэтапов

Следующий чат не должен начинать с большого кода. Рекомендуемая последовательность:

```text
0. Зафиксировать базовый runtime fixture.
   - изучить актуальные LLM routing classes;
   - посмотреть один полный log и diagnostics;
   - подтвердить актуальный status mapping.

1. Маленький подэтап: исправить UI NPE selectedItem == null.
   - unit/UI-level safety fix;
   - build + запуск.

2. Маленький подэтап: typed provider failure classification.
   - без circuit breaker и без большой миграции на первом шаге;
   - build + unit tests.

3. Circuit breaker для Ollama crash и OpenRouter 429.
   - проверить на 3-5 READY вакансий;
   - не запускать 47, пока не подтверждён cooldown.

4. Retryable persistence / queue semantics.
   - перенос 429/empty provider failures из FAILED;
   - хранение nextRetryAt и failure metadata.

5. Strict cover letter mode и DOM audit.
   - приложить diagnostics минимум 3 случаев previous partial success;
   - поправлять selectors только по фактическому DOM.

6. Только затем обсуждать и проектировать разделение prepare/apply pipeline.
```

---

## 10. Файлы, которые нужно изучить в новом чате перед патчами

### LLM и routing

```text
LlmPort.java
LlmGenerationOptions.java
LlmProviderUnavailableException.java
Llm routing / fallback service classes
OllamaLlmAdapter.java
OllamaChatRequest.java
OllamaChatResponse.java
OllamaProperties.java
OpenRouterLlmAdapter.java
OpenRouter request/response DTOs
OpenRouter properties / retry policy
GenerateCoverLetterService.java
```

### HH execution / queue

```text
HhBrowserAutoResponseAgent.java
HhBrowserAutoResponseResult.java
HhBrowserAutoResponseOutcome.java
HhAutoResponseExecutionAdapter.java
AutoResponseExecutionService.java
AutoResponseExecutionResultDto.java
AutoResponseExecutionStatus.java
AutoResponseQueueItem.java
AutoResponseQueueStatus.java
AutoResponseQueueRepository.java
AutoResponseQueueRepositoryAdapter.java
AutoResponseQueueItemEntity.java
AutoResponseQueueItemPersistenceMapper.java
SpringDataAutoResponseQueueItemJpaRepository.java
Flyway migrations for queue
```

### Batch and UI

```text
ReadyAutoResponseBatchService.java
AutoResponseBatchProgressStore.java
AutoResponseBatchExecutionConfiguration.java
AutoResponsesController.java
auto-responses.fxml
ReadyAutoResponseBatchServiceTest.java
AutoResponseExecutionServiceTest.java
```

### Questionnaire

```text
GenerateHhQuestionnaireAnswersService.java
HhQuestionnaireFormPromptContext.java
HhQuestionnaireQuestionDto.java
HhQuestionnaireOptionDto.java
HhQuestionnaireFieldType.java
GeneratedHhQuestionnaireAnswerDto.java
GeneratedHhQuestionnaireAnswersDto.java
HhQuestionnaireGenerationContext.java
prompts/hh-questionnaire/form-answer-system.j2
prompts/hh-questionnaire/form-answer-user.j2
```

---

## 11. Runtime-артефакты, необходимые для точечной диагностики cover letter

Для следующей сессии нужно приложить, не скрывая важные технические детали, но обязательно удалив секреты:

```text
1. Полный последний application log.
2. ZIP минимум трёх папок:
   logs/hh-browser-debug/<vacancyId>/<executionId>/
   где reason=response-sent-without-cover-letter-form.
3. Полную папку diagnostics успешного response-sent-with-cover-letter.
4. Актуальные файлы HhBrowserAutoResponseAgent и HhAutoResponseExecutionAdapter.
5. Актуальные LLM adapters/routing и application config без ключей.
6. Результат:
   mvnw.cmd clean test -pl core,infrastructure,ui,app -am
```

Нельзя прикладывать:

```text
- OPENROUTER_API_KEY;
- HH OAuth access/refresh tokens;
- HH client secret;
- Chrome cookies/profile;
- другие пароли, ключи и персональные secrets.
```

---

## 12. Что не делать в следующем чате

```text
- Не переписывать Selenium selectors по одному логу без HTML/PNG diagnostics.
- Не считать 12 PARTIAL_SUCCESS полноценными успехами.
- Не оставлять OpenRouter 429 как FAILED вакансии.
- Не делать повторные попытки Ollama для каждой вакансии после явного CUDA/PTX crash.
- Не отправлять отклик без подтверждённого письма в default policy.
- Не убирать qwen3:14b из primary target architecture.
- Не возвращать HhQuestionnaireAnswerGroundingGate как блокирующий business-decider.
- Не отправлять форму, если технический DOM-contract невалиден.
- Не просить повторно уже приложенные файлы в рамках одной сессии:
  сначала использовать историю, архивы и текущие attachments.
- Не переходить к следующему подэтапу до build/test/real runtime confirmation пользователя.
```

---

## 13. Последняя подтверждённая точка

```text
- Все подэтапы batch-queue, candidate approval, atomic claim,
  backend batch worker и UI progress были успешно собраны Maven.
- Реальный batch на 47 вакансиях завершился и не нарушил последовательность worker.
- Результат показывает архитектурную необходимость стабилизировать LLM routes
  и ввести строгую policy cover letter до дальнейших массовых запусков.
- Следующий осмысленный подэтап: не selector-fix, а runtime diagnosis +
  typed provider failure classification/circuit breaker.
```

---

## 14. Короткий итог

```text
Сделано:
- HH OAuth, поиск, очередь и Selenium auto-response;
- candidate approval status с diagnostics path;
- массовая выборка и READY batch action;
- READY → IN_PROGRESS atomic claim;
- single-worker sequential batch runner;
- UI запуск batch и live-progress;
- реальный запуск 47 вакансий.

Главная проблема:
- Ollama primary падает по CUDA/PTX;
- fallback OpenRouter ограничен 429 и иногда даёт пустой output;
- 12 откликов были отправлены без подтверждённой формы письма;
- текущий result model смешивает retryable provider failures и настоящие FAILED.

Следующая цель:
- стабилизировать LLM route, ввести typed failures/circuit breaker/retry semantics,
  затем strict cover letter + DOM audit, затем разделить generation и apply pipeline.
```
