# JobHunterPro Desktop Application — полный отчёт по текущему чату

**Дата составления:** 2026-06-22  
**Проект:** `JobHunterPro Desktop Application`  
**Локальный путь:** `C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application`  
**Назначение:** передать контекст следующему чату без потери технических решений, текущих результатов, ограничений и точки остановки.

> **Правило для следующего чата:** не угадывать актуальную структуру проекта и не писать патчи по устаревшим версиям классов. Если для решения нужен конкретный класс, тест, конфигурация, миграция, FXML или шаблон — сначала попросить пользователя приложить актуальный файл.

---

## 1. Как начать новый чат

В новый чат нужно приложить этот отчёт и написать:

```text
Продолжаем разработку JobHunterPro Desktop Application.

Прочитай приложенный markdown-отчёт и используй его как основной контекст.
Не гадай по отсутствующим или старым файлам: если для следующего шага нужен конкретный актуальный класс, тест, YAML, миграция, FXML или Jinja-шаблон — попроси меня приложить его.

Мы уже перевели LLM-контур на Ollama как основной provider и OpenRouter как fallback, вынесли промпты в .j2, реализовали evidence-grounded заполнение текстовых анкет HH.ru и безопасный review-flow.

Текущая проблема:
fallback Ollama → OpenRouter технически переключается корректно, но OpenRouter иногда возвращает пустой content. Из-за этого подготовка сопроводительного письма завершается FAILED, и очередь переводится в FAILED. Нужно сделать устойчивую обработку OpenRouter empty content и корректный lifecycle очереди для временных LLM-сбоев.

Сначала изучи раздел «Текущая точка остановки» и запроси актуальные файлы, перечисленные в разделе «Файлы, необходимые для продолжения».
```

---

## 2. Ссылка на текущий чат

Я не могу получить или создать фактическую ссылку Share на текущий чат изнутри среды ChatGPT.

```text
Добавь ссылку на этот чат вручную из браузера перед сохранением отчёта.
```

---

## 3. Главные продуктовые и архитектурные принципы

### 3.1. Основной сценарий продукта

`JobHunterPro` должен оставаться продуктом про настоящие автоотклики.

```text
Поиск вакансий
→ очередь автооткликов
→ подготовка сопроводительного письма и ответов анкеты
→ browser automation
→ отправка отклика.
```

Ручная проверка допустима как safety-механизм для нестандартных анкет, но не должна превращать продукт в обычный ручной процесс «открой вакансию и откликнись сам».

### 3.2. Архитектура

```text
core/
- domain, DTO, use cases, outbound ports;
- без Spring, JPA, JavaFX, Selenium, OkHttp.

infrastructure/
- persistence, HTTP, LLM providers, browser automation,
  prompt rendering, external integrations.

ui/
- JavaFX;
- работает через use cases из core.

app/
- Spring Boot bootstrap и application configuration.
```

### 3.3. Технологический стек

```text
Java 21
JavaFX 21
Spring Boot 3.5.x
Maven multi-module
PostgreSQL 16 на localhost:5434
Flyway
Hibernate/JPA
OkHttp 5.4.0
Jackson
Selenium WebDriver
JUnit 5
PDFBox
Jsoup
Jinjava 2.8.0
Ollama
OpenRouter
```

---

## 4. Что было готово до этого чата

К началу этого чата уже работали:

```text
- HH.ru OAuth через jobhunterpro://oauth/hh/callback;
- сохранение HH access token в external_auth_tokens;
- HH connection status = CONNECTED;
- получение текущего пользователя HH.ru;
- поиск вакансий и очередь автооткликов;
- загрузка primary PDF resume;
- получение подробностей вакансии HH.ru;
- Selenium browser auto-response;
- генерация сопроводительного письма;
- заполнение текстовой анкеты HH.ru в review-режиме;
- диагностические HTML, PNG и questionnaire-review.json.
```

Habr Career остаётся отложенным из-за внешних ограничений приложения/API.

---

## 5. Candidate Questionnaire Profile и quality gate

В этом чате уже реализован профиль фактов кандидата, который используется для безопасного заполнения повторяющихся вопросов работодателя.

### 5.1. Профиль кандидата

Добавлены профиль, persistence, DTO, use cases, UI и Flyway-миграции для данных:

```text
- часовой пояс;
- минимальные и максимальные зарплатные ожидания;
- валюта;
- gross/net basis;
- готовность к переезду;
- предпочтительный формат работы;
- приоритет удалённой работы;
- уровень английского;
- готовность к командировкам;
- срок готовности начать работу;
- additional confirmed facts;
- allowRelatedExperienceDrafts.
```

Текущие migrations для этого функционала находятся после прежних миграций проекта. В истории обсуждались V6/V7/V8; перед любым новым изменением схемы нужно запросить актуальный список Flyway-файлов.

### 5.2. Результат анкеты

Для каждого текстового вопроса формируется:

```text
fieldName
answer
quality:
- CONFIRMED
- REVIEW_REQUIRED
reviewReason
evidence
```

Принцип:

```text
CONFIRMED:
- факт подтверждён структурированным профилем;
  либо
- найдена проверяемая цитата из resume / additional confirmed facts.

REVIEW_REQUIRED:
- прямого подтверждения нет;
- финальная отправка анкеты не выполняется;
- диагностические артефакты сохраняются;
- задача остаётся доступной для последующего review-flow.
```

### 5.3. Структурированные вопросы

Для вопросов о зарплате, переезде, английском, командировках, сроке выхода, формате работы и часовом поясе используются данные профиля, а не LLM.

Пример корректного подтверждённого результата:

```text
Вопрос: «Какой уровень заработной платы вы рассматриваете?»
Ответ: «Рассматриваю зарплату в диапазоне 90000–150000 RUB в месяц.»
Evidence: PROFILE:Зарплатные ожидания 90000–150000 RUB
```

---

## 6. Evidence-grounded flow для технических вопросов

### 6.1. Проблема, которая была устранена

Ранее LLM могла подтвердить технический опыт без доказательства или, наоборот, не заметить Java в резюме.

Примеры нежелательного поведения:

```text
- Cucumber мог быть отмечен как CONFIRMED без подтверждения;
- Java могла стать REVIEW_REQUIRED, хотя Java явно есть в resume;
- модель могла вернуть «требует уточнения кандидатом» прямо в поле анкеты.
```

### 6.2. Текущая схема

```text
Текст вопроса HH.ru
→ structured profile resolver
→ single-question LLM proposal
→ Java grounding gate
→ direct resume evidence extractor
→ CONFIRMED или REVIEW_REQUIRED
→ Selenium fill in review mode.
```

LLM больше не является единственным источником решения о подтверждённости.

### 6.3. Одно поле — один LLM request

Большой JSON-массив с ответами на все вопросы был заменён на отдельную генерацию на каждый вопрос.

Причина:

```text
Free OpenRouter models иногда обрывали большой JSON-массив.
```

Текущая схема:

```text
Один вопрос HH.ru
→ один компактный JSON-объект
→ retry только этого поля при невалидном JSON.
```

Ожидаемый контракт предложения:

```json
{
  "topic": "Java",
  "answer": "Краткий ответ либо пустая строка",
  "evidenceSource": "RESUME",
  "evidenceQuote": "Точная непрерывная цитата из источника"
}
```

Допустимые `evidenceSource`:

```text
RESUME
PROFILE_ADDITIONAL
NONE
```

### 6.4. HhQuestionnaireAnswerGroundingGate

Класс:

```text
infrastructure/src/main/java/
ru/jobhunter/infrastructure/service/
HhQuestionnaireAnswerGroundingGate.java
```

Основные обязанности:

```text
- deterministic resolution facts из Candidate Questionnaire Profile;
- проверка evidenceQuote;
- проверка, что quote действительно встречается в исходном resume;
- проверка topic/question/evidence;
- выбор безопасного CONFIRMED answer;
- генерация REVIEW_REQUIRED для неподтверждённых вопросов;
- логирование причины отклонения grounding.
```

### 6.5. Direct resume evidence extractor

Добавлен:

```text
QuestionnaireResumeEvidenceCandidateExtractor.java
DirectResumeEvidenceCandidate
```

Идея:

```text
Java-код сам ищет прямую именованную сущность из вопроса
в непрерывном фрагменте резюме.

Это не список захардкоженных технологий.
Работает по общему правилу literal matching значимых
именованных терминов из вопроса.
```

Примеры:

```text
Java в вопросе + Java/Spring Boot в resume
→ direct RESUME evidence
→ CONFIRMED.

Selenium/Selenide отсутствуют в resume
→ REVIEW_REQUIRED.

Cucumber/BDD отсутствует в resume
→ REVIEW_REQUIRED.
```

### 6.6. Подтверждённый последний результат

Последний успешный `questionnaire-review.json` показал:

```text
provider = ollama
model = qwen3:4b

confirmedFilledCount = 2
reviewFilledCount = 2
reviewSkippedCount = 0
```

Итог:

```text
1. Salary
   CONFIRMED через PROFILE.

2. Java
   CONFIRMED через точную цитату из resume:
   «3+ года опыта разработки масштабируемых Enterprise-систем
   и высоконагруженных веб-сервисов (Java/Spring Boot).»

3. Selenium/Selenide
   REVIEW_REQUIRED, так как прямой опыт в resume не указан.

4. Cucumber/BDD
   REVIEW_REQUIRED, так как прямой опыт в resume не указан.
```

Это целевой безопасный результат для текущей анкеты.

---

## 7. Review-ответы для неподтверждённых вопросов

Старые технические заглушки были заменены на честные, читабельные review-черновики.

Примеры текущего поведения:

```text
Прямое подтверждение уровня владения Java в резюме не найдено.
Готов обсудить релевантный опыт и задачи на интервью.
```

```text
Прямой опыт работы с Selenium, Selenide в резюме не указан.
Готов обсудить смежный опыт и применимость навыков к задачам проекта.
```

Важно:

```text
Система больше не пишет неподтверждённое
«знаком на базовом уровне».
```

Отдельные regexp-паттерны в gate извлекают предмет из типовых формулировок вопроса, а не используют `topic`, сгенерированный LLM, как готовую грамматическую часть предложения.

---

## 8. LLM-подсистема: Ollama primary, OpenRouter fallback

### 8.1. Причина рефакторинга

OpenRouter free-модели показали нестабильность:

```text
- 429 rate limit;
- пустой response content;
- обрезанный JSON;
- нестрогий structured output.
```

Поэтому принято продуктовое решение:

```text
Ollama — основной локальный LLM provider.
OpenRouter — резервный внешний provider.
```

### 8.2. Ollama локально

Ollama установлен и доступен через локальный API:

```text
http://localhost:11434
```

Рабочая модель:

```text
qwen3:4b
```

Тестовая локальная модель `gemma4:latest` также была обнаружена, но в текущем LLM flow использовался `qwen3:4b`.

### 8.3. Routing architecture

Введён внутренний provider contract и единый внешний `LlmPort`.

Ориентировочная структура:

```text
infrastructure/llm/routing/
- LlmProvider
- LlmProviderUnavailableException
- LlmRoutingPort
- LlmRoutingConfiguration
- UnavailableLlmPort

infrastructure/llm/ollama/
- OllamaProperties
- OllamaLlmClient
- OllamaLlmAdapter
- request/response DTO.

infrastructure/llm/openrouter/
- OpenRouterLlmAdapter
- OpenRouterLlmClient
- OpenRouterProperties
- OpenRouter exceptions/DTO.
```

Цепочка:

```text
GenerateCoverLetterService
GenerateHhQuestionnaireAnswersService
→ LlmPort
→ LlmRoutingPort
→ Ollama primary
→ OpenRouter fallback только при provider-unavailable failure.
```

### 8.4. Важное исправление Spring beans

После появления router одновременно создавались:

```text
llmPort
stubLlmPort
```

Это приводило к ошибке Spring:

```text
No qualifying bean of type LlmPort:
expected single matching bean but found 2
```

Исправление:

```text
StubLlmPortConfiguration.java удалён.
```

Не возвращать этот bean без пересмотра всей конфигурации, иначе снова появится неоднозначность `LlmPort`.

### 8.5. Environment variables

Нормальная конфигурация после теста fallback:

```text
OLLAMA_ENABLED=true
OLLAMA_MODEL=qwen3:4b
OLLAMA_BASE_URL=http://localhost:11434

OPENROUTER_ENABLED=true
OPENROUTER_API_KEY=<SECRET>
OPENROUTER_PRIMARY_MODEL=openrouter/free
OPENROUTER_FALLBACK_MODELS=
```

Никогда не хранить в Git, отчётах, логах или скриншотах:

```text
OPENROUTER_API_KEY
OAuth secrets
HH access/refresh tokens
browser cookies
```

---

## 9. Проверенная работа Ollama

Ollama была проверена end-to-end:

```text
- Spring startup:
  LLM routing configured:
  primaryProvider=ollama,
  fallbackProvider=not-configured.

- cover letter:
  Calling Ollama model:
  useCase=generate-cover-letter,
  model=qwen3:4b.

- questionnaire:
  Calling Ollama model:
  useCase=answer-hh-questionnaire,
  model=qwen3:4b.
```

В последующих проверках все системные и пользовательские prompt templates рендерились успешно, текстовая анкета заполнялась, а финальный статус оставался безопасным:

```text
QUESTIONNAIRE_FILLED_REVIEW_REQUIRED
```

---

## 10. Внешние `.j2` prompts

### 10.1. Причина

Inline prompts были вынесены из Java-классов, чтобы:

```text
- отделить бизнес-логику от prompt design;
- использовать Jinja-compatible шаблоны;
- проще тестировать и улучшать prompts;
- не смешивать HTTP/provider-код и prompt content.
```

### 10.2. Используемый движок

```text
Jinjava 2.8.0
```

### 10.3. Prompt infrastructure

Добавлены/используются:

```text
PromptTemplate
PromptTemplateModel
PromptTemplateRenderer
PromptTemplateRenderingException
ClasspathJinjaPromptTemplateRenderer
CoverLetterPromptContext
HhSingleQuestionPromptContext
```

### 10.4. Пути шаблонов

```text
infrastructure/src/main/resources/prompts/

cover-letter/
- system.j2
- user.j2

hh-questionnaire/
- single-answer-system.j2
- single-answer-user.j2
```

### 10.5. Template rendering tests

Добавлен тест renderer:

```text
ClasspathJinjaPromptTemplateRendererTest
```

Он проверяет:

```text
- шаблоны доступны из classpath;
- Jinja variables рендерятся;
- в prompt не остаются строки {{ variable_name }}.
```

### 10.6. Prompting rules

Перед созданием `.j2` пользователем были приложены правила prompt engineering. Их нужно сохранять как стандарт для будущих prompt changes:

```text
- чёткая роль, цель и границы;
- контекст в отдельных XML-like блоках;
- явная иерархия источников;
- строгий контракт structured output;
- запрет на неподтверждённые факты;
- server-side validation остаётся в Java;
- examples добавлять только при доказанной пользе;
- prompts не должны содержать бизнес-решения, которые должны жить в коде.
```

Перед созданием новых промптов в новом чате не нужно заново требовать эти правила, если приложен этот отчёт. Но при существенном изменении продукта или policy лучше уточнить у пользователя дополнительные ограничения.

---

## 11. Cover letter output policy

### 11.1. Проблема

Ollama могла игнорировать инструкцию в prompt и вернуть письмо длиной около 3600–3900 символов, хотя prompt требовал максимум 1400.

Одного prompt недостаточно. Ограничение должно быть на стороне Java.

### 11.2. Реализованное решение

В `LlmGenerationOptions` добавлены специальные режимы:

```text
coverLetter():
temperature = 0.3
maxTokens = 420
responseFormat = TEXT

compactCoverLetter():
temperature = 0.0
maxTokens = 300
responseFormat = TEXT
```

`GenerateCoverLetterService` теперь:

```text
1. Генерирует письмо.
2. Очищает code fences / whitespace.
3. Проверяет:
   - текст не blank;
   - длина ≤ 1400 символов.
4. При превышении лимита выполняет одну повторную компактную генерацию.
5. При повторном нарушении завершает pipeline исключением output policy.
6. Не отдаёт слишком длинное письмо в Selenium.
```

### 11.3. Подтверждённый результат

Последний обычный Ollama run выдал:

```text
coverLetterLength = 1353
```

Это соответствует серверному лимиту ≤ 1400.

---

## 12. Текущее поведение browser flow

### 12.1. Анкеты

Для текстовых анкет:

```text
- Selenium извлекает вопросы;
- профильные вопросы заполняются подтверждёнными фактами;
- LLM + evidence gate обрабатывают технические вопросы;
- все поля заполняются;
- diagnostics сохраняются;
- final questionnaire submit намеренно не нажимается;
- результат:
  QUESTIONNAIRE_FILLED_REVIEW_REQUIRED.
```

### 12.2. Очередь

При `QUESTIONNAIRE_FILLED_REVIEW_REQUIRED` очередь остаётся `READY`.

Это не ошибка: пока нет отдельного жизненного цикла review/approval, задача сохраняется доступной для повторного запуска после решения пользователя.

Нужный будущий lifecycle:

```text
READY
→ QUESTIONNAIRE_FILLED_REVIEW_REQUIRED
→ пользователь проверяет/подтверждает ответы
→ повторный browser flow
→ финальная отправка анкеты
→ SENT.
```

Не включать автоматический final submit анкеты, пока такой lifecycle и явное подтверждение не реализованы.

---

## 13. Проверка реального Ollama → OpenRouter fallback

### 13.1. Как проводился тест

Для проверки router был намеренно задан неверный endpoint:

```text
OLLAMA_BASE_URL=http://127.0.0.1:11435
```

При этом OpenRouter был включён.

### 13.2. Что успешно

Лог подтвердил:

```text
LLM routing configured:
primaryProvider=ollama,
fallbackProvider=openrouter

Calling Ollama model:
useCase=generate-cover-letter,
model=qwen3:4b

Primary LLM provider is unavailable.
Trying fallback provider:
primary=ollama,
fallback=openrouter,
failureType=OllamaLlmException

Calling OpenRouter model:
useCase=generate-cover-letter,
model=openrouter/free
```

Следовательно, routing Ollama → OpenRouter работает корректно.

### 13.3. Что сломалось

OpenRouter затем вернул:

```text
OpenRouter response content is empty
```

Этот сбой не был обработан как временная ошибка provider-а в cover letter pipeline.

Итог:

```text
HH.ru auto response preparation failed
→ AutoResponseExecutionService
→ queue item status = FAILED.
```

Браузер в этом тесте не запускался, фактический отклик не отправлялся.

---

## 14. Текущая точка остановки

### Главная незакрытая задача

Нужно сделать устойчивый fallback и корректный queue lifecycle при временной ошибке LLM provider.

Текущее неверное поведение:

```text
Ollama недоступна
→ router переключается на OpenRouter
→ OpenRouter возвращает empty content
→ задача становится FAILED.
```

Нужное поведение:

```text
Ollama недоступна
→ OpenRouter fallback
→ OpenRouter empty content / retryable provider error
→ retry на fallback-модели или controlled unavailable result
→ задача не должна необратимо превращаться в FAILED
   из-за временной недоступности LLM.
```

### Что нужно реализовать

1. В `OpenRouterLlmAdapter` / client:
   - классифицировать empty content как provider-unavailable / retryable failure;
   - использовать внутренний fallback model list, если он уже предусмотрен;
   - сделать controlled final exception после исчерпания моделей/попыток.

2. В routing:
   - убедиться, что provider failure корректно пробрасывается как typed failure;
   - не маскировать ошибки модели как обычный business failure.

3. В execution pipeline:
   - отличать временную проблему LLM от реального browser/HH failure;
   - для LLM unavailable:
     - не переводить queue item в FAILED;
     - оставить READY либо ввести отдельный retryable status;
     - записывать понятную причину в diagnostics/logs.

4. После исправления:
   - повторить fallback test с `OLLAMA_BASE_URL=127.0.0.1:11435`;
   - убедиться, что OpenRouter используется;
   - проверить сценарий empty content;
   - вернуть:
     `OLLAMA_BASE_URL=http://localhost:11434`.

### Важное состояние окружения

После последнего fallback теста нужно убедиться, что пользователь вернул адрес Ollama:

```text
OLLAMA_BASE_URL=http://localhost:11434
```

---

## 15. Что не делать на следующем шаге

```text
- Не включать final questionnaire submit автоматически.
- Не считать Selenium/Selenide/Cucumber подтверждёнными без evidence.
- Не использовать @Primary, чтобы скрыть ошибку нескольких LlmPort beans.
- Не возвращать StubLlmPortConfiguration без изменения router architecture.
- Не логировать или показывать API keys/tokens/cookies.
- Не делать большой монолитный патч без актуальных файлов.
- Не менять current prompt policy без учёта приложенных user prompting rules.
```

---

## 16. Файлы, необходимые для продолжения

Перед реализацией устойчивого OpenRouter fallback и правильной обработки очереди запросить актуальные версии:

```text
OpenRouterLlmAdapter.java
OpenRouterLlmClient.java
OpenRouterProperties.java
OpenRouterLlmException.java
LlmRoutingPort.java
LlmProvider.java
LlmProviderUnavailableException.java
HhAutoResponseExecutionAdapter.java
AutoResponseExecutionService.java
AutoResponseExecutionResultDto.java
AutoResponseExecutionStatus.java
GenerateCoverLetterService.java
```

При необходимости также:

```text
LlmRoutingConfiguration.java
OllamaLlmAdapter.java
OllamaLlmException.java
configuration class с OkHttp beans
application.yaml без секретов
AutoResponseQueueItem domain/entity/repository/service classes
```

---

## 17. Подтверждённые тесты и проверки

Пользователь подтверждал успешные Maven builds после основных фаз рефакторинга.

Типовая команда:

```powershell
mvn clean test -pl core,infrastructure,ui,app -am
```

Также использовались/обновлялись:

```text
GenerateCoverLetterServiceTest
ClasspathJinjaPromptTemplateRendererTest
QuestionnaireResumeEvidenceCandidateExtractorTest
```

При будущем исправлении fallback обязательно добавить тесты на:

```text
- Ollama provider unavailable → OpenRouter called;
- OpenRouter empty content → retryable typed exception;
- оба provider-а unavailable → controlled unavailable result;
- queue не становится FAILED при temporary LLM failure;
- реальный browser/HH failure по-прежнему может становиться FAILED.
```

---

## 18. Краткий итог

```text
Сделано:
- Candidate Questionnaire Profile;
- evidence-grounded quality gate;
- direct resume evidence for named skills;
- Ollama as local primary LLM;
- OpenRouter as fallback provider;
- LLM routing;
- external .j2 prompts through Jinjava;
- cover letter server output policy;
- безопасный questionnaire review flow;
- подтверждённый Java evidence;
- честные REVIEW_REQUIRED ответы для Selenium/Cucumber.

Работает:
- Ollama qwen3:4b генерирует cover letters и questionnaire proposals;
- profile facts и Java evidence подтверждаются;
- анкета заполняется и сохраняет diagnostics;
- final submit анкеты не выполняется намеренно;
- Ollama → OpenRouter routing технически переключается.

Где остановились:
- OpenRouter fallback иногда возвращает empty content;
- это ошибочно переводит queue item в FAILED;
- нужно сделать retryable LLM failure handling и правильный lifecycle очереди.
```
