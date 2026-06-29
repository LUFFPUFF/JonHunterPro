# JobHunterPro Desktop Application — отчёт для перехода в новый чат

**Дата составления:** 2026-06-23  
**Проект:** `JobHunterPro Desktop Application`  
**Локальный путь проекта:** `C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application`  
**Назначение:** передать следующему чату весь необходимый контекст по реализации автооткликов HH.ru, LLM-подсистеме и текущей проблеме с полными анкетами.

> **Критическое правило для следующего чата:** не угадывать текущее состояние исходников. В этом чате несколько классов менялись последовательно, часть патчей могла быть применена частично. Перед любым новым патчем сначала изучить актуальные файлы, приложенные пользователем, и последний runtime-лог.

---

## 1. Короткий стартовый промпт для нового чата

В новый чат приложить этот отчёт и отправить:

```text
Продолжаем разработку JobHunterPro Desktop Application.

Прочитай приложенный отчёт полностью и используй его как основной контекст.
Наша текущая цель — кардинально стабилизировать LLM-first заполнение анкет HH.ru: LLM должна принимать смысловые решения по всей форме, а Java/Selenium должны только извлекать форму, валидировать структурированный ответ и применять его в DOM.

Не гадай по старым версиям классов и не предлагай очередные точечные заплатки до изучения актуальных файлов и последнего тестового результата. Сначала проанализируй разделы «Текущая точка остановки», «Что обязательно приложить» и приложенные исходники/логи. Затем предложи диагностический и архитектурный план, после чего давай изменения небольшими проверяемыми этапами.

Важно:
- Ollama qwen3:14b должна оставаться primary-моделью;
- OpenRouter остаётся fallback;
- HhQuestionnaireAnswerGroundingGate не должен отменять смысловые решения LLM в execution-path;
- LLM не должна выдумывать проверяемые факты;
- продукт должен оставаться про реальные автоотклики, а не про ручной review как основной сценарий.
```

---

## 2. Ссылка на текущий чат

Фактическую Share-ссылку на этот чат нужно добавить вручную из браузера, если она нужна в архиве проекта.

---

## 3. Продуктовая цель и ключевые ограничения

`JobHunterPro` — desktop-приложение для настоящих автооткликов на вакансии.

Целевой поток:

```text
Поиск вакансий
→ очередь автооткликов
→ подготовка сопроводительного письма и ответов анкеты
→ Selenium browser automation
→ отправка отклика
→ SENT.
```

Продуктовые решения, которые нельзя потерять:

```text
- Основная ценность — автоматические отклики, а не ручная работа с вакансиями.
- Ручная проверка может быть safety fallback, но не центральным потоком продукта.
- HH.ru — активная интеграция.
- Habr Career временно отложен из-за внешних ограничений OAuth/API.
- Не намеренно выдумывать проверяемые факты о стаже, технологиях, образовании,
  работодателях, проектах, документах и личных обстоятельствах.
- Предпочтения кандидата: зарплата, формат, офис/удалёнка, переезд, командировки,
  тестовое, срок выхода — могут использоваться LLM как входные факты для решения.
```

---

## 4. Архитектура и окружение

### 4.1. Модули

```text
core/
- domain, DTO, use cases, ports;
- без Spring, Selenium, JavaFX, JPA и HTTP-клиентов.

infrastructure/
- persistence, LLM providers, prompt rendering, Selenium/HH browser agent,
  HTTP clients и адаптеры внешних сервисов.

ui/
- JavaFX-контроллеры и FXML.

app/
- Spring Boot bootstrap и конфигурация.
```

### 4.2. Стек

```text
Java 21
JavaFX 21
Spring Boot 3.5.x
Maven multi-module
PostgreSQL 16: localhost:5434/jobhunter
Flyway
Hibernate / JPA
Selenium WebDriver
OkHttp 5
Jackson
Jinjava 2.8.0
Ollama
OpenRouter
JUnit 5
PDFBox
Jsoup
```

### 4.3. Важные локальные настройки

```text
Windows account: beats
Проект: C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application
PostgreSQL port: 5434
HH OAuth redirect: jobhunterpro://oauth/hh/callback
Custom-scheme forward port: 54347
```

---

## 5. Что работало до текущего этапа

Подтверждённо работали:

```text
- HH.ru OAuth через jobhunterpro://oauth/hh/callback;
- сохранение HH access token в external_auth_tokens;
- HH connection status = CONNECTED;
- HH current user API check;
- поиск HH-вакансий;
- очередь автооткликов;
- загрузка и хранение primary resume;
- получение подробностей вакансии;
- Selenium browser auto-response;
- вставка и отправка сопроводительного письма;
- распознавание успешного отклика по UI HH;
- сохранение диагностических HTML/PNG/JSON в logs/hh-browser-debug/<executionId>/.
```

Browser-agent использует Chrome-профиль пользователя, чтобы работать в уже авторизованной HH-сессии.

---

## 6. История LLM-подсистемы

### 6.1. Первое состояние

Ранее использовался OpenRouter как основной provider. Были проблемы:

```text
- rate limit 429;
- empty content;
- нестабильный JSON;
- периодические ошибки free-маршрута;
- слишком слабая предсказуемость при анкетах.
```

### 6.2. Принятое решение

```text
Ollama — primary local provider.
OpenRouter — fallback provider.
```

Это решение сохраняется.

### 6.3. Локальные модели

Установлены:

```text
qwen3:4b
qwen3:14b
gemma3:12b
gemma4:latest
```

### 6.4. Почему заменили qwen3:4b

`qwen3:4b` часто возвращала planning/reasoning вместо готового текста сопроводительного письма, даже при `think=false`.

Для неё поток часто был:

```text
Ollama qwen3:4b
→ reasoning / unsafe output
→ output-policy reject
→ OpenRouter fallback.
```

### 6.5. Проверка qwen3:14b напрямую через Ollama API

Выполнен raw smoke-test `/api/chat`:

```text
model: qwen3:14b
stream: false
think: false
format: json
temperature: 0
num_predict: 300
num_ctx: 8192
```

Результат:

```text
message.content:
{"selectedOptionValue": 2, "selectionReason": "..."}

message.thinking:
пусто

done:
true

done_reason:
stop
```

Вывод:

```text
- think=false фактически применяется;
- final content приходит отдельно от thinking;
- qwen3:14b умеет вернуть JSON;
- локальный API Ollama и модель исправно работают.
```

Нюанс: модель вернула `selectedOptionValue` числом, а не строкой. Это стало одной из причин отказаться от требования LLM возвращать реальные DOM value как технический идентификатор.

---

## 7. Улучшения Ollama adapter

В этом чате были внесены/предложены изменения в Ollama-интеграцию.

Цель:

```text
request:
- stream=false;
- think=false;
- temperature=0 для structured questionnaire answers;
- JSON / JSON Schema, где это оправдано;
- num_ctx из конфигурации;
- keep_alive из конфигурации.

response:
- message.content — единственный бизнес-ответ;
- message.thinking — только техническая диагностика;
- done_reason, usage и latency — в безопасных логах;
- пустой content — typed provider failure / fallback.
```

Актуальный `OllamaProperties` расширялся параметрами:

```text
enabled
baseUrl
model
connectTimeoutSeconds
readTimeoutSeconds
writeTimeoutSeconds
contextLength
keepAlive
```

Для `qwen3:14b` предполагалась конфигурация:

```text
OLLAMA_MODEL=qwen3:14b
OLLAMA_CONTEXT_LENGTH=8192
OLLAMA_KEEP_ALIVE=10m
OLLAMA_READ_TIMEOUT_SECONDS=300
```

Важно: не предполагать, что все эти изменения окончательно применены без проверки текущих файлов.

---

## 8. Candidate Questionnaire Profile

В проект добавлен профиль фактов кандидата. Он нужен для повторяющихся HR-вопросов.

Данные профиля:

```text
- часовой пояс;
- диапазон зарплаты;
- валюта;
- gross/net basis;
- готовность к переезду;
- предпочтительный формат работы;
- приоритет удалённой работы;
- уровень английского;
- готовность к командировкам;
- готовность к тестовому заданию;
- срок готовности начать работу;
- additional confirmed facts;
- allowRelatedExperienceDrafts.
```

В последнем реальном логе профиля были значения:

```text
salary: 90 000–150 000 RUB
salaryTaxBasis: GROSS
remoteWorkPriority: true
relocationReady: false
businessTripsReady: false
englishLevel: B2
workFormatPreference: ANY
testAssignmentReadiness: UNKNOWN
timeZone: Europe/Moscow
```

Проблема старого flow: детерминированный код мог вставить неуместный ответ по профилю без учёта конкретного вопроса вакансии. Например, на вопрос об офисе 9:00–18:00 в Санкт-Петербурге был вставлен общий текст про любой формат и приоритет удалёнки, что семантически не отвечало на вопрос.

---

## 9. Старый evidence-grounded pipeline

До текущего LLM-first эксперимента существовал безопасный text-flow:

```text
Вопрос HH.ru
→ structured profile resolver
→ single-question LLM proposal
→ HhQuestionnaireAnswerGroundingGate
→ direct resume evidence extractor
→ CONFIRMED / REVIEW_REQUIRED
→ Selenium review flow.
```

### 9.1. Полезные элементы, которые уже есть

```text
HhQuestionnaireAnswerGroundingGate
QuestionnaireResumeEvidenceCandidateExtractor
DirectResumeEvidenceCandidate
CandidateQuestionnaireProfileFacts
```

Они умеют:

```text
- определять подтверждённость фактов;
- находить буквальные совпадения терминов в резюме;
- безопасно работать с Java, Selenium, Cucumber и другими навыками;
- не подтверждать факт без evidence.
```

### 9.2. Почему старый flow больше не подходит

`HhQuestionnaireAnswerGroundingGate` был блокирующим:

```text
LLM формирует ответ
→ gate может заменить ответ или сделать REVIEW_REQUIRED
→ agent пропускает поле
→ форма не отправляется.
```

Это конфликтует с выбранным направлением:

```text
LLM должна быть главным интерпретатором формы.
Java должна валидировать контракт и выполнять Selenium-действия,
а не отменять смысловой ответ LLM.
```

### 9.3. Текущее решение по gate

Не удалять его сразу, но:

```text
- убрать из execution-path;
- оставить потенциально как audit / risk diagnostics;
- не давать ему права переписывать или блокировать ответ LLM.
```

Сохраняется ограничение: LLM не должна сочинять проверяемые биографические/профессиональные факты.

---

## 10. Реальные результаты тестов до form-level refactor

### 10.1. Успешные отклики

Были подтверждённые реальные успешные Selenium-отклики: cover letter генерировался и вставлялся, затем HH подтверждал отклик.

Улучшена проверка success UI:

```text
- visible text «Вы откликнулись»;
- кнопка чата [data-qa='vacancy-response-link-view-topic'].
```

Это снизило риск ложного FAILED после реального отклика.

### 10.2. Результаты множества прогонов анкет

При анализе 12 реальных запусков были наблюдения:

```text
SUCCESS: 7
PARTIAL_SUCCESS: 2
Анкета остановила отклик: 3
```

Это не означает, что анкеты были полностью корректно обработаны: часть успешных запусков относилась к вакансиям без сложных форм.

### 10.3. Проблемы реальных форм

Обнаружены формы:

```text
- TEXT;
- RADIO;
- RADIO + «Свой вариант» + textarea;
- CHECKBOX;
- SELECT.
```

Старая реализация умела ограниченно работать с `TEXT` и `RADIO`.

Сложный паттерн:

```text
Да / Нет / Свой вариант + textarea
```

нужно поддерживать как:

```text
RADIO_WITH_OTHER_TEXT
```

`CHECKBOX` и `SELECT` пока должны быть безопасно остановлены до submit, пока не реализованы полноценно.

---

## 11. Текущий целевой LLM-first pipeline

Нужный поток:

```text
HH DOM
→ извлечение единой схемы формы
→ один LLM-запрос на всю форму
→ структурированный результат
→ Java schema / DOM validation
→ Selenium fill
→ проверка фактического DOM состояния
→ submit
→ SENT.
```

### 11.1. Разделение ответственности

LLM должна:

```text
- понимать смысл всех вопросов вместе;
- выбирать radio-варианты;
- формировать textarea-ответы;
- решать, когда использовать «Свой вариант»;
- учитывать резюме, профиль и вакансию как контекст.
```

Java должна:

```text
- извлекать реальные поля и доступные варианты из HH DOM;
- сопоставлять структурированный ответ с вопросами;
- проверять, что выбранный option реально существует;
- проверять обязательность текста для «Свой вариант»;
- заполнять textarea/radio;
- убеждаться, что DOM действительно обновился;
- нажимать submit только при полном валидном наборе ответов.
```

---

## 12. Первый form-level generator

Создан/добавлен новый подход в `GenerateHhQuestionnaireAnswersService`.

Первый вариант контракта заставлял LLM вернуть:

```json
{
  "answers": [
    {
      "fieldName": "task_356313708_text",
      "answer": "Текст",
      "selectedOptionValue": "",
      "reason": "Основание"
    }
  ]
}
```

Это оказалось неудачной архитектурной границей.

### 12.1. Почему первый вариант не сработал

Реальный тест с четырьмя текстовыми вопросами дал две проблемы:

1. Первая попытка `qwen3:14b` закончилась:

```text
doneReason=length
completionTokens=600
```

JSON оказался обрезанным.

2. На повторной попытке модель завершилась с `doneReason=stop`, но вернула шаблонные идентификаторы:

```text
task_123_text
task_456
task_789
```

вместо реальных:

```text
task_356313708_text
task_356313709_text
task_356313710_text
task_356313711_text
```

Java-валидатор правильно остановил форму:

```text
LLM must return an answer for every questionnaire field
```

Ни Selenium-заполнение, ни submit в этом сценарии не выполнялись.

### 12.2. Причина

System prompt содержал демонстрационные `task_123...`; `qwen3:14b` копировала технические примеры вместо реальных DOM field names.

Главный вывод:

```text
LLM не должна отвечать техническими HH task_* field names
и реальными option value.
```

---

## 13. Последняя предложенная архитектурная коррекция: questionIndex / optionIndex

Вместо DOM-идентификаторов LLM должна отвечать индексами, а Java должна делать техническое сопоставление.

Новый целевой контракт:

```json
{
  "answers": [
    {
      "questionIndex": 1,
      "answer": "Краткий текстовый ответ",
      "selectedOptionIndex": 0
    },
    {
      "questionIndex": 2,
      "answer": "",
      "selectedOptionIndex": 3
    }
  ]
}
```

Правила:

```text
TEXT:
- questionIndex = порядковый номер вопроса;
- answer непустой;
- selectedOptionIndex = 0.

RADIO:
- answer = "";
- selectedOptionIndex = реальный индекс варианта 1..N.

RADIO_WITH_OTHER_TEXT:
- selectedOptionIndex = индекс варианта;
- если выбран otherOptionIndex, answer непустой;
- иначе answer = "".
```

Java затем делает:

```text
questionIndex 1
→ questions.get(0)
→ настоящий fieldName из DOM

selectedOptionIndex 3
→ question.options().get(2)
→ настоящий radio value из DOM.
```

Преимущество:

```text
LLM сохраняет власть над смыслом ответа.
Код не интерпретирует смысл — только делает безопасное техническое связывание.
```

### 13.1. Изменения, которые были предложены

Планировалось/частично применялось:

```text
- HhQuestionnaireFormPromptContext:
  конвертирует реальные вопросы/варианты в PromptQuestion(questionIndex, ...)
  и PromptOption(optionIndex, ...).

- form-answer-system.j2:
  запрещает task_*, fieldName и реальные option value;
  требует только questionIndex и selectedOptionIndex.

- form-answer-user.j2:
  передаёт question_index и option_index.

- GenerateHhQuestionnaireAnswersService:
  парсит questionIndex/selectedOptionIndex;
  проверяет полноту и уникальность индексов;
  сама сопоставляет индексы с реальными HhQuestionnaireQuestionDto и options.

- HhQuestionnaireFieldType:
  должен поддерживать TEXT, RADIO, RADIO_WITH_OTHER_TEXT.

- HhQuestionnaireQuestionDto:
  должен нести:
  fieldName,
  questionText,
  fieldType,
  options,
  otherOptionValue,
  otherTextFieldName.
```

### 13.2. Статус

Этот переход **не подтверждён финальной успешной сборкой и runtime-прогоном**.

В процессе были исправлены две compile-проблемы:

```text
1. HhQuestionnaireQuestionDto:
   compact constructor нельзя завершать return.
   Решение: использовать явный canonical constructor с final local variables
   и присваиванием полей через this.fieldName = ... .

2. GenerateHhQuestionnaireAnswersService:
   был вызов toGeneratedAnswers(...), но самого метода не было.
   Метод был добавлен отдельно.
```

После этого пользователь сообщил, что проблема всё ещё сохраняется, но полный последний runtime-результат перед переходом в новый чат не приложен.

---

## 14. Текущая точка остановки

### 14.1. Текущая проблема

Формы HH.ru с несколькими вопросами по-прежнему нестабильны в LLM-first pipeline.

Из уже подтверждённых причин:

```text
- qwen3:14b медленно отвечает на большие form-level prompts;
- JSON может обрезаться при лимите генерации;
- модель может копировать демонстрационные/технические идентификаторы;
- текущие исходники могли быть частично изменены несколькими последовательными патчами;
- нет подтверждённого успешного end-to-end run после перехода на questionIndex.
```

Пользователь хочет решить проблему в новом чате более кардинально, а не продолжать цепочку точечных исправлений.

### 14.2. Что важно сделать в новом чате в первую очередь

Не начинать с нового большого патча. Сначала:

```text
1. Изучить текущие исходники и финальный runtime-лог.
2. Зафиксировать одну реальную форму как воспроизводимый test fixture:
   - извлечённые вопросы;
   - реальный prompt;
   - raw LLM response;
   - ожидаемое структурированное представление.
3. Определить, где именно ломается путь:
   - extractor;
   - prompt context;
   - Ollama response;
   - JSON parsing;
   - Java mapping;
   - Selenium filling;
   - submit.
4. Написать/добавить unit test на mapping реального fixture
   до очередной правки browser-agent.
5. После этого выбрать окончательный контракт формы.
```

### 14.3. Предпочтительный радикальный подход

Рекомендуемое направление для обсуждения в новом чате:

```text
- исключить task_* и DOM value из LLM output;
- использовать questionIndex / optionIndex либо вообще positional array;
- оставить LLM один полный смысловой запрос по форме;
- держать prompt кратким;
- не включать повторяющиеся instruction-heavy examples;
- ограничивать output по количеству и формату;
- строить отдельный deterministic mapper и тестировать его на реальных fixtures;
- не возвращать HhQuestionnaireAnswerGroundingGate в блокирующий execution-path;
- не выполнять submit, если хотя бы один ответ не прошёл чистую техническую validation.
```

Параллельно стоит оценить, действительно ли один тяжёлый запрос на всю форму — оптимальный компромисс для локального CPU. Пользователь выбрал LLM-first направление, но конкретная стратегия batching (вся форма одним запросом или небольшие смысловые группы) должна быть подтверждена benchmark-ом на реальных формах, а не предположением.

---

## 15. Последний тестовый результат, который нужно обязательно приложить в новом чате

Пользователь должен приложить **результат последней попытки после всех актуальных изменений**, даже если она закончилась ошибкой.

Минимальный набор:

```text
1. Полный лог запуска приложения:
   от нажатия «Выполнить/Откликнуться» до конечного статуса.
   Нельзя вырезать строки Ollama/OpenRouter, GenerateHhQuestionnaireAnswersService,
   HhBrowserAutoResponseAgent и AutoResponseExecutionService.

2. Полная папка:
   logs/hh-browser-debug/<executionId>/
   либо ZIP этой папки.

3. Все артефакты запуска:
   - questionnaire-review.json, если создан;
   - HTML diagnostic;
   - PNG screenshot;
   - execution-error / questionnaire-generation-unavailable файлы, если есть.

4. Скриншот самой формы HH.ru до отправки или screenshot из diagnostics,
   на котором видны вопросы и варианты.

5. Текущие исходники после последней правки:
   - HhBrowserAutoResponseAgent.java;
   - HhQuestionnaireQuestionDto.java;
   - HhQuestionnaireFieldType.java;
   - HhQuestionnaireOptionDto.java;
   - GenerateHhQuestionnaireAnswersService.java;
   - HhQuestionnaireFormPromptContext.java;
   - GeneratedHhQuestionnaireAnswerDto.java;
   - GeneratedHhQuestionnaireAnswersDto.java;
   - HhQuestionnaireGenerationContext.java;
   - LlmGenerationOptions.java;
   - OllamaLlmAdapter.java;
   - OllamaChatRequest.java;
   - OllamaChatResponse.java;
   - PromptTemplate.java;
   - prompts/hh-questionnaire/form-answer-system.j2;
   - prompts/hh-questionnaire/form-answer-user.j2.

6. Итог команды:
   mvnw.cmd clean test -pl core,infrastructure,ui,app -am
```

Перед отправкой нельзя включать:

```text
- OPENROUTER_API_KEY;
- HH client secret;
- OAuth access/refresh tokens;
- cookies Chrome/HH;
- другие ключи и пароли.
```

---

## 16. Где находятся диагностические артефакты

Browser-agent сохраняет артефакты в:

```text
logs/hh-browser-debug/<executionId>/
```

Типовые имена:

```text
questionnaire-review.json
questionnaire-filled-review-required-<timestamp>-session-<sessionId>.html
questionnaire-filled-review-required-<timestamp>-session-<sessionId>.png
questionnaire-generation-unavailable-<timestamp>-session-<sessionId>.html
questionnaire-generation-unavailable-<timestamp>-session-<sessionId>.png
questionnaire-unsupported-field-type-<timestamp>-session-<sessionId>.html
questionnaire-unsupported-field-type-<timestamp>-session-<sessionId>.png
execution-error-<timestamp>-session-<sessionId>.html
execution-error-<timestamp>-session-<sessionId>.png
```

---

## 17. Последняя подтверждённая успешная сборка

До последних незафиксированных questionIndex-правок была успешная Maven-сборка:

```text
BUILD SUCCESS

JobHunterPro Core: SUCCESS
JobHunterPro Infrastructure: SUCCESS
JobHunterPro UI: SUCCESS
JobHunterPro App: SUCCESS

Tests:
- Core: 16
- Infrastructure: 71
- Всего: 87
- Failures: 0
- Errors: 0
- Skipped: 1 (expected OpenRouter real smoke-test)
```

Типовая команда:

```powershell
mvnw.cmd clean test -pl core,infrastructure,ui,app -am
```

Предупреждения, не блокирующие текущую сборку:

```text
- Mockito dynamic agent;
- JAVA_TOOL_OPTIONS / IBM866;
- тестовые WARN OpenRouter и HH API в negative test cases;
- JavaFX dependency model warnings.
```

---

## 18. Что не делать в новом чате

```text
- Не строить патчи по устаревшему report без актуальных файлов.
- Не возвращать qwen3:4b как production primary.
- Не исключать Ollama из questionnaire route без причины:
  qwen3:14b должна оставаться primary.
- Не давать HhQuestionnaireAnswerGroundingGate право отменять LLM-ответы
  в execution-path.
- Не заставлять LLM генерировать task_* / реальный DOM option value.
- Не включать автоматический submit для неполной или невалидной формы.
- Не скрывать ошибки LLM за generic FAILED без diagnostics.
- Не отправлять секреты в архиве/чате.
- Не превращать продукт в ручной «review-only» инструмент.
```

---

## 19. Краткий итог

```text
Сделано:
- HH OAuth, поиск вакансий, очередь и Selenium auto-response;
- генерация и отправка cover letter;
- Candidate Questionnaire Profile;
- evidence-grounded text flow и diagnostics;
- Jinja .j2 prompts;
- Ollama routing as primary and OpenRouter fallback;
- тестирование qwen3:14b через Ollama API;
- qwen3:14b подтверждённо возвращает final content без thinking;
- начат переход на form-level LLM-first processing;
- определена необходимость RADIO_WITH_OTHER_TEXT;
- выявлена неверная граница: LLM не должна генерировать технические HH fieldName/value.

Работает:
- qwen3:14b доступна, think=false работает;
- Maven-сборка базового form-level этапа проходила;
- browser diagnostics и safety-stop сохраняются;
- Selenium-автоотклик работает на обычных вакансиях.

Где остановились:
- form-level questionnaire pipeline ещё не стабилен;
- пользователь сообщает, что ошибка сохраняется;
- результат последнего теста после актуальных правок нужно приложить;
- следующий чат должен начать с диагностики конкретного runtime fixture
  и затем сделать чистый, проверяемый redesign mapping/contract.
```
