# JobHunterPro Desktop Application — полный отчёт по текущему чату

Ссылка на чат: https://chatgpt.com/c/6a315204-58d4-83eb-ac85-f252a1d4b123

Дата составления: 2026-06-16  
Проект: `JobHunterPro Desktop Application`  
Локальный путь пользователя: `C:\Users\beats\IdeaProjects\JobHunterPro-Desktop_Application`  
Назначение файла: передать следующий чат, чтобы он сразу понял, что было сделано, что получилось, что не получилось, почему мы остановились на автооткликах и почему следующий этап — подключение LLM-модели.

> Важное правило для следующего чата: если какой-то файл, метод, package, DTO, migration, FXML или реализация не видны напрямую, нужно попросить пользователя приложить актуальный файл. Не нужно угадывать структуру проекта, потому что код активно менялся.

---

## 1. Как использовать этот отчёт в новом чате

В новом чате лучше отправить этот markdown-файл и написать:

```text
Продолжаем разработку JobHunterPro Desktop Application.

Прочитай приложенный markdown-отчёт.
Не гадай по отсутствующим файлам: если нужен актуальный код, попроси приложить файл.

Мы остановились после успешного proof-of-concept HH.ru browser auto-response agent:
- вакансии ищутся через HH API application token;
- официальный HH API для резюме и отклика оказался заблокирован 403 forbidden;
- отправка отклика через браузерный агент Selenium заработала;
- Selenium Chrome profile авторизован в HH.ru;
- базовый сценарий отклика проходит;
- вакансии с вопросами работодателя пока не поддержаны.

Следующий этап:
подключение LLM-модели, её тестирование и интеграция в приложение.
LLM нужна для генерации сопроводительных писем и в будущем для ответов на вопросы работодателя в формах отклика.
```

---

## 2. Ссылка на текущий чат

Я не могу получить или сгенерировать реальную share-ссылку на текущий чат изнутри среды ChatGPT.

Добавь её вручную через кнопку **Share / Поделиться** в интерфейсе ChatGPT:

```text
Ссылка на текущий чат: ДОБАВИТЬ_ВРУЧНУЮ_ССЫЛКУ_НА_ШАРИНГ_ЧАТА
```

---

## 3. Стартовая точка этого чата

В начале текущего чата пользователь попросил продолжать по:

```text
JobHunterPro_Master_Prompt
JobHunterPro_dialogue_report1
JobHunterPro_full_dialogue_report2_2026-06-14
JobHunterPro_full_dialogue_report3_2026-06-16
```

Актуальной точкой остановки из предыдущего отчёта было:

```text
HH.ru:
- developer application одобрено;
- real OAuth работает;
- custom URI callback работает;
- access/refresh token сохраняются в external_auth_tokens;
- HH connection status = CONNECTED;
- GetHhCurrentUserUseCase и HH API /me работают.

AutoResponses:
- очередь вакансий работает;
- статусы QUEUED / READY / SENT / FAILED / SKIPPED уже введены;
- READY-фильтр работает;
- execution pipeline работает;
- HhAutoResponseExecutionAdapter был NOT_AVAILABLE stub.

Следующий шаг:
2.3.1 — проверить официальный HH endpoint для отправки отклика.
2.3.2 — реализовать настоящий HhAutoResponseExecutionAdapter вместо NOT_AVAILABLE stub.
```

Важный продуктовый принцип остаётся прежним:

```text
JobHunterPro должен оставаться продуктом про настоящие автоотклики.
Ручной сценарий “открой вакансию и сам откликнись” не должен становиться основным продуктовым путём.
Допустимы только временные safety/dev fallback-сценарии.
```

---

## 4. Общий технологический контекст

Проект: desktop-приложение для поиска вакансий, подготовки очереди откликов и автоматизации отправки откликов.

Стек:

```text
Java 21+
JavaFX 21
Spring Boot 3.x headless
Maven multi-module
PostgreSQL
Flyway
Hibernate/JPA
OkHttp
Jackson
JUnit 5
MockWebServer
Selenium WebDriver
Virtual threads / applicationTaskExecutor
```

Модули:

```text
jobhunter-pro/
├── pom.xml
├── core/
├── infrastructure/
├── ui/
└── app/
```

Архитектурные правила:

```text
core не зависит от Spring, JPA, Hibernate, JavaFX, OkHttp, Selenium.
ui не зависит от infrastructure напрямую.
infrastructure реализует интерфейсы/use cases/repositories из core.
app стартует JavaFX + Spring Boot headless.
UI работает через use case интерфейсы из core.
```

---

## 5. Главные результаты текущего чата

В этом чате мы:

```text
1. Проверили официальный HH API по отправке отклика.
2. Выяснили, что HH API можно использовать для поиска вакансий, но applicant-действия закрыты.
3. Исправили поиск вакансий через HH application token.
4. Проверили /resumes/mine — получили 403 forbidden.
5. Проверили POST /negotiations — получили 403 forbidden.
6. Сделали вывод, что официальное API не даёт текущему приложению отправлять отклики.
7. Перешли к browser-agent подходу.
8. Реализовали Selenium-based HH Browser Auto Response Agent.
9. Настроили отдельный persistent Chrome profile для HH.ru.
10. Авторизовали Selenium-профиль в HH.ru.
11. Довели базовый browser-flow до успешного отклика.
12. Выявили отдельный неподдержанный сценарий: вакансии с вопросами работодателя.
13. Решили остановить развитие автооткликов на текущем стабильном этапе и перейти к LLM.
```

---

## 6. 2.3 — исследование реального HH auto-response через официальный API

### 6.1. Проверка документации HH API

Пользователь дал ссылку:

```text
https://api.hh.ru/openapi/redoc#section/Obshaya-informaciya
```

Сначала был сделан план:

```text
2.3.1 — проверить endpoint отправки отклика.
2.3.2 — добавить POST JSON в HhApiRequestExecutor.
2.3.3 — добавить DTO request body.
2.3.4 — реализовать HhApiClient.applyToVacancy(...).
2.3.5 — заменить HhAutoResponseExecutionAdapter stub на реальную реализацию.
```

Изначальное предположение было:

```text
POST /vacancies/{vacancyId}/application
body:
{
  "resume_id": "...",
  "message": "..."
}
```

После проверки на практике этот путь дал:

```text
HTTP 404
```

Вывод:

```text
/vacancies/{id}/application — неверный route для текущего HH API.
```

Дальше был использован более подходящий endpoint:

```text
POST /negotiations
body:
{
  "resume_id": "...",
  "vacancy_id": "...",
  "message": "..."
}
```

Этот endpoint существовал, но вернул:

```json
{"errors":[{"type":"forbidden"}],"request_id":"..."}
```

Вывод:

```text
endpoint существует, но действие отклика запрещено текущему приложению/токену.
```

---

## 7. Исправление поиска вакансий через HH application token

### 7.1. Проблема

После перехода к автооткликам пользователь попробовал поиск вакансий и получил:

```text
HH API request failed with HTTP status 403
```

Скрин показывал:

```text
HH API доступен через /me.
Но поиск Java / area 113 падал с 403.
```

Причина:

```text
HhApiClient.searchVacancies(...) вызывал requestExecutor.getPublic(...).
То есть /vacancies запрашивался как anonymous/public request.
```

### 7.2. Решение

Было принято решение использовать:

```text
HH application access token
```

Были добавлены/предложены:

```text
HhApplicationTokenProperties
jobhunter.integrations.hh.application.access-token
HH_APPLICATION_ACCESS_TOKEN
HhApiClient.searchVacanciesAuthorized(...)
HhVacancySearchService теперь использует application token
```

Блок настроек:

```yaml
jobhunter:
  integrations:
    hh:
      application:
        access-token: ${HH_APPLICATION_ACCESS_TOKEN:}
```

После получения application token через HH и настройки env-переменной поиск заработал:

```text
Поиск завершен.
Показано вакансий: 20.
Найдено: 1845.
```

Вывод:

```text
HH API оставляем для поиска вакансий, потому что это самый ресурсоёмкий участок и он успешно работает через application token.
```

---

## 8. Получение HH application token

Пользователь не понимал, где взять `HH_APPLICATION_ACCESS_TOKEN`.

Было объяснено:

```text
Client ID и Client Secret видны в dev.hh.ru.
Application access token нужно сгенерировать отдельно.
```

PowerShell-команда:

```powershell
$clientId = 'ТВОЙ_CLIENT_ID'
$clientSecret = 'ТВОЙ_CLIENT_SECRET'

$body = @{
  grant_type = 'client_credentials'
  client_id = $clientId
  client_secret = $clientSecret
}

$response = Invoke-RestMethod `
  -Method Post `
  -Uri 'https://api.hh.ru/token' `
  -ContentType 'application/x-www-form-urlencoded' `
  -Headers @{ 'User-Agent' = 'JobHunterPro/0.1.0 (email@example.com)' } `
  -Body $body

$response.access_token
```

Пользователь сначала получил ошибку, потому что вставил `client_id` и `client_secret` без кавычек. Было пояснено:

```text
В PowerShell строковые значения нужно брать в кавычки.
```

Важно:

```text
Пользователь показывал Client Secret на скрине/в тексте.
После отладки лучше перевыпустить Client Secret в кабинете HH.
В отчёт реальные значения не включать.
```

---

## 9. Проверка /resumes/mine

План был:

```text
1. Добавить GetHhResumesUseCase.
2. Добавить HhResumeDto.
3. Добавить HhResumeService.
4. Добавить HhApiClient.getMyResumes(...).
5. В ProfileController добавить кнопку “Проверить резюме HH.ru”.
6. Получить resume_id для будущей отправки отклика.
```

Были предложены классы:

```text
HhResumeDto
GetHhResumesUseCase
HhResumeStatusResponse
HhResumeItemResponse
HhMineResumesResponse
HhResumeService
```

Метод в `HhApiClient`:

```java
public CompletableFuture<HhMineResumesResponse> getMyResumes(String accessToken) {
    return requestExecutor.getAuthorized(
            "/resumes/mine",
            Map.of(),
            accessToken,
            HhMineResumesResponse.class
    );
}
```

Результат:

```json
{"errors":[{"type":"forbidden"}],"request_id":"1781620173638acd32d1ee4669381004"}
```

Вывод:

```text
access token валидный, /me работает, но HH запрещает /resumes/mine.
```

---

## 10. Проверка POST /negotiations

После 404 на `/vacancies/{id}/application` метод был исправлен на:

```text
POST /negotiations
```

DTO был расширен:

```text
resume_id
vacancy_id
message
```

Пример request body:

```json
{
  "resume_id": "...",
  "vacancy_id": "134229036",
  "message": "Здравствуйте! Откликаюсь на вакансию..."
}
```

Результат:

```json
{"errors":[{"type":"forbidden"}],"request_id":"1781621006171797b4ddfa0026955002"}
```

Логика execution pipeline при этом сработала правильно:

```text
READY -> запуск adapter -> HH API 403 -> AutoResponseExecutionResultDto.FAILED -> queue status FAILED
```

Вывод:

```text
Официальный HH API для applicant auto-apply заблокирован правами HH.
Ошибка не в Java-коде и не в JSON.
```

---

## 11. Итог по официальному HH API

### Что получилось

```text
HH OAuth пользователя работает.
HH API /me работает.
HH application token работает.
Поиск вакансий через API работает.
Execution pipeline работает.
```

### Что не получилось

```text
GET /resumes/mine -> 403 forbidden.
POST /negotiations -> 403 forbidden.
```

### Вывод

```text
HH API используем для поиска вакансий.
Реальную отправку откликов делаем через browser-agent.
```

---

## 12. Переход к Selenium browser-agent

Пользователь предложил:

```text
Если API можно использовать исключительно для поиска вакансий, это хорошо.
Процесс поиска ресурсоёмкий, API помогает.
Раз отклики через API запретили, реализуем агента, который будет заходить на вакансию, вставлять сопроводительное письмо и откликаться.
Другие сценарии добавим позже.
```

Был согласован новый этап:

```text
2.4 — HH Browser Auto Response Agent
```

Цель:

```text
API остаётся для поиска вакансий.
Очередь автооткликов остаётся текущая.
ExecuteAutoResponseUseCase остаётся текущий.
HhAutoResponseExecutionAdapter теперь выполняет отклик через браузер.
Пользователь заранее один раз входит в HH.ru в persistent Chrome profile.
Агент открывает vacancyUrl, нажимает “Откликнуться”, вставляет cover letter и отправляет.
```

---

## 13. Selenium dependency

В infrastructure был добавлен Selenium:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
</dependency>
```

Selenium Manager сам подбирает chromedriver.

---

## 14. Настройки browser-agent

Был предложен класс:

```text
HhBrowserAutoResponseProperties
```

Prefix:

```text
jobhunter.integrations.hh.browser-auto-response
```

Основные параметры:

```text
enabled
dryRun
headless
userDataDir
defaultMessage
waitTimeoutSeconds
```

YAML-блок:

```yaml
jobhunter:
  integrations:
    hh:
      browser-auto-response:
        enabled: ${HH_BROWSER_AUTO_RESPONSE_ENABLED:true}
        dry-run: ${HH_BROWSER_AUTO_RESPONSE_DRY_RUN:true}
        headless: ${HH_BROWSER_AUTO_RESPONSE_HEADLESS:false}
        user-data-dir: ${HH_BROWSER_USER_DATA_DIR:${user.home}/.jobhunterpro/hh-chrome-profile}
        wait-timeout-seconds: ${HH_BROWSER_WAIT_TIMEOUT_SECONDS:30}
        default-message: ${HH_BROWSER_DEFAULT_MESSAGE:Здравствуйте! Откликаюсь на вакансию "{vacancyName}". Буду рад обсудить детали.}
```

Позже для тестирования было рекомендовано:

```yaml
dry-run: false
headless: false
wait-timeout-seconds: 10
```

---

## 15. HhBrowserDriverFactory

Был создан/предложен:

```text
HhBrowserDriverFactory
```

Назначение:

```text
Создаёт ChromeDriver с отдельным persistent profile.
```

Важные Chrome arguments:

```text
--user-data-dir=<HH_BROWSER_USER_DATA_DIR>
--profile-directory=Default
--start-maximized
--disable-notifications
--headless=new при headless=true
```

Профиль:

```text
C:\Users\beats\.jobhunterpro\hh-chrome-profile
```

---

## 16. HhBrowserAutoResponseAgent

### 16.1. Первая версия

Первая версия:

```text
1. Открывала vacancyUrl.
2. Искала кнопку “Откликнуться”.
3. Кликала.
4. Искала textarea.
5. Вставляла cover letter.
6. В dry-run режиме не нажимала submit.
```

Проблема:

```text
Агент очень долго ждал textarea.
Причина: каждый selector ждал по 30 секунд.
При 5 selector-ах получалось около 150 секунд.
```

Ошибка:

```text
HH cover letter textarea was not found.
```

### 16.2. Улучшенная версия

Была предложена улучшенная версия:

```text
- быстрый поиск selector-ов без ожидания по 30 сек на каждый;
- общий deadline;
- диагностика screenshot/html;
- проверка login;
- поддержка кнопки “Добавить сопроводительное письмо”;
- JavaScript click;
- сохранение diagnostics в logs/hh-browser-debug.
```

Добавлены проверки:

```text
ensureLoggedIn(...)
findFirstVisibleNow(...)
findFirstVisibleWithDeadline(...)
saveDiagnostics(...)
```

---

## 17. Проблема anonymous-сессии HH

Симптом:

```text
Агент открывал вакансию, но в браузере были кнопки “Войти” и “Создать резюме”.
```

HTML debug показывал:

```text
userType: "" || 'anonymous'
login: ""
cryptedUserId: ""
```

Вывод:

```text
OAuth-токен JobHunterPro и web-сессия Chrome — разные вещи.
Selenium Chrome profile должен быть отдельно авторизован в HH.ru.
```

---

## 18. Запуск Chrome с Selenium-профилем

Изначальная команда не сработала, потому что Chrome был не по пути:

```text
C:\Users\beats\AppData\Local\Google\Chrome\Application\chrome.exe
```

Было предложено найти Chrome:

```powershell
$chromeCandidates = @(
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
  "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
  "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
)

$chrome = $chromeCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
$chrome
```

И запустить:

```powershell
& $chrome `
  --user-data-dir="$env:USERPROFILE\.jobhunterpro\hh-chrome-profile" `
  --profile-directory=Default `
  "https://hh.ru"
```

Альтернатива:

```powershell
Start-Process chrome.exe -ArgumentList @(
  "--user-data-dir=$env:USERPROFILE\.jobhunterpro\hh-chrome-profile",
  "--profile-directory=Default",
  "https://hh.ru"
)
```

После этого пользователь вошёл в HH.ru в Selenium-профиле.

---

## 19. Успешная авторизация Selenium-профиля

После ручного входа debug HTML показывал:

```text
userType: "applicant" || 'anonymous'
login: "beatsluffi@gmail.com"
hhid: "93379656"
```

Это означало:

```text
Selenium Chrome profile теперь авторизован как applicant.
```

---

## 20. Первый успешный browser-flow

Пользователь показал скрин:

```text
Резюме доставлено
Связаться с работодателем можно в чате
textarea с сопроводительным письмом
кнопка “Отправить”
toast: “Отклик отправлен”
```

Это подтвердило:

```text
Агент реально дошёл до successful HH response state.
```

В debug HTML был признак:

```text
data-qa="vacancy-response-success-standard-notification"
```

и текст:

```text
Отклик отправлен
```

Вывод:

```text
Базовый browser-flow автоотклика работает.
```

---

## 21. Почему некоторые элементы очереди были FAILED, хотя на странице успех

Обнаружилось:

```text
На dry-run adapter возвращал NOT_AVAILABLE или exception-подобный результат.
AutoResponseExecutionService маппит SUCCESS -> SENT, FAILED/NOT_AVAILABLE -> FAILED.
Поэтому dry-run мог пометить очередь как FAILED, даже если Selenium дошёл до формы/успешного состояния.
```

Было предложено:

```text
На dry-run временно возвращать SUCCESS, чтобы очередь не уходила в FAILED.
Позже добавить отдельный статус DRY_RUN_COMPLETED.
```

---

## 22. Уточнение реального поведения HH

Выяснилось, что HH может вести себя по-разному:

### Сценарий A — отклик сразу отправляется

```text
Нажали “Откликнуться”.
HH сразу показывает “Резюме доставлено” / “Отклик отправлен”.
Сопроводительное письмо можно отправить уже как сообщение в чат.
```

### Сценарий B — появляется поле сопроводительного письма до отправки

```text
Нажали “Откликнуться”.
Появилась форма.
Агент вставляет письмо.
Нажимает submit.
```

### Сценарий C — работодатель задаёт вопросы

```text
Нажали “Откликнуться”.
HH показывает форму с вопросами работодателя.
Агент пока не должен угадывать ответы.
Такую вакансию нужно пометить как неподдержанный/ручной сценарий.
```

---

## 23. Финальная логика HhBrowserAutoResponseAgent

Была предложена версия, которая:

```text
1. Открывает vacancyUrl.
2. Проверяет, что HH userType = applicant.
3. Ищет кнопку “Откликнуться”.
4. Кликает.
5. Проверяет, не появился ли уже success state:
   - “Отклик отправлен”
   - “Резюме доставлено”
   - “Вы уже откликались”
   - data-qa='vacancy-response-success-standard-notification'
6. Если success есть:
   - пытается найти textarea чата;
   - вставляет cover letter;
   - при dryRun=false нажимает “Отправить”;
   - возвращает success.
7. Если success нет:
   - проверяет наличие формы вопросов;
   - если вопросы есть — возвращает понятную ошибку “scenario not supported yet”.
8. Если есть textarea до отправки:
   - вставляет cover letter;
   - при dryRun=false нажимает submit;
   - ждёт success.
9. При ошибках сохраняет screenshot/html.
```

Важные методы:

```text
isResponseAlreadySent(...)
handlePostResponseMessage(...)
hasQuestionForm(...)
waitForResponseSent(...)
ensureLoggedIn(...)
findFirstVisibleWithDeadline(...)
saveDiagnostics(...)
```

---

## 24. Текущее рабочее состояние автооткликов

К концу чата пользователь написал:

```text
Сейчас все окей проходит.
```

Текущий статус:

```text
Поиск вакансий через HH API работает.
Очередь автооткликов работает.
READY pipeline работает.
Selenium browser-agent открывает нужную вакансию.
Selenium Chrome profile авторизован в HH.ru.
Базовый сценарий автоотклика проходит.
Сопроводительное письмо может быть вставлено/отправлено в поддерживаемом сценарии.
Вакансии с вопросами работодателя пока не поддержаны.
```

---

## 25. Что решено НЕ делать прямо сейчас

Пользователь предложил остановиться на этапе автооткликов:

```text
Предлагаю сейчас остановиться на этапе авто откликов,
ведь мы можем проходить тесты с помощью LLM,
тут же мы решаем проблему с генерацией соп письма,
поэтому предлагаю переходить к подключение LLM модели,
тестирование ее и далее интеграции в приложение.
Делать мы это будем в следующем чате.
```

Решение:

```text
Автоотклики пока считаем доведёнными до рабочего MVP/proof-of-concept.
Дальше не углубляемся в Selenium handler-ы.
Следующий крупный этап — LLM.
```

---

## 26. Что предстоит сделать по автооткликам позже

### 26.1. Статусы очереди

Сейчас есть:

```text
QUEUED
READY
SENT
FAILED
SKIPPED
```

Полезно добавить позже:

```text
QUESTIONNAIRE_REQUIRED
LOGIN_REQUIRED
NEEDS_MANUAL_ACTION
DRY_RUN_COMPLETED
PLATFORM_BLOCKED
```

Особенно важно:

```text
Вакансия с вопросами работодателя не должна считаться обычным FAILED.
Это отдельный ожидаемый сценарий.
```

### 26.2. QuestionnaireHandler

Позже нужен отдельный handler:

```text
HhQuestionnaireHandler
```

Минимальная версия:

```text
1. Обнаружить форму с вопросами.
2. Считать вопросы.
3. Сохранить их в DTO/log/UI.
4. Пометить вакансию как QUESTIONNAIRE_REQUIRED.
5. Не отправлять отклик автоматически.
```

Расширенная версия после LLM:

```text
1. Считать вопросы работодателя.
2. Передать в LLM вместе с профилем/резюме пользователя.
3. Сгенерировать ответы.
4. Показать ответы пользователю или отправить автоматически только при включённом режиме.
```

### 26.3. Улучшение browser-agent

Позже:

```text
- выделить отдельные handler-ы по сценариям страницы;
- меньше завязываться на текстовые XPath;
- добавить Page Object;
- добавить retries;
- добавить остановку при капче/подозрительной активности;
- добавить лимиты и паузы;
- сделать batch flow с отчётом.
```

---

## 27. Почему следующий этап — LLM

LLM нужна сразу для двух ключевых задач:

```text
1. Генерация сопроводительного письма.
2. Ответы на вопросы работодателя в формах отклика.
```

Сейчас шаблон письма простой:

```text
Здравствуйте! Откликаюсь на вакансию "{vacancyName}". Буду рад обсудить детали.
```

Но для реального продукта нужен более качественный текст:

```text
- учитывать название вакансии;
- учитывать компанию;
- учитывать стек/требования;
- учитывать опыт пользователя;
- не писать длинно;
- не выглядеть как общий шаблон;
- адаптироваться под Junior/Middle/Senior;
- отвечать на вопросы работодателя аккуратно.
```

---

## 28. Рекомендуемый следующий этап

### 28.1. Название этапа

```text
2.5 — LLM integration for cover letter and questionnaire answers
```

### 28.2. Цели этапа

```text
1. Выбрать способ подключения LLM.
2. Протестировать модель отдельно от приложения.
3. Создать core-порты и use case.
4. Реализовать infrastructure adapter.
5. Интегрировать генерацию сопроводительного письма в AutoResponses.
6. Позже использовать LLM для вопросов работодателя.
```

---

## 29. Варианты LLM-подключения, которые нужно обсудить в следующем чате

### Вариант A — облачный API

```text
Плюсы:
- лучше качество;
- проще интеграция;
- не грузит ПК;
- быстрее старт.

Минусы:
- нужен API key;
- стоимость;
- данные вакансий/резюме уходят во внешний сервис;
- нужна настройка безопасности.
```

### Вариант B — локальная модель через Ollama/LM Studio

```text
Плюсы:
- данные остаются локально;
- удобно для дипломного/desktop-приложения;
- можно показать автономность.

Минусы:
- качество зависит от модели;
- нужна память/CPU/GPU;
- медленнее;
- сложнее подобрать русскоязычную модель.
```

### Вариант C — локальный microservice

```text
Java app вызывает локальный HTTP server:
POST /generate-cover-letter
POST /answer-questionnaire
```

Плюсы:

```text
- Java-приложение не зависит от конкретной LLM-библиотеки;
- можно менять модель без изменения core;
- проще тестировать через MockWebServer;
- соответствует clean architecture.
```

---

## 30. Рекомендуемая архитектура LLM-интеграции

### 30.1. Core DTO

Предложить в следующем чате:

```text
GenerateCoverLetterCommand
GeneratedCoverLetterDto
AnswerEmployerQuestionCommand
EmployerQuestionAnswerDto
```

Пример `GenerateCoverLetterCommand`:

```java
public record GenerateCoverLetterCommand(
        UserId userId,
        VacancySource source,
        String externalVacancyId,
        String vacancyName,
        String employerName,
        String areaName,
        String vacancyDescription,
        String userProfileSummary,
        String resumeSummary
) {}
```

### 30.2. Core port

```java
public interface CoverLetterGenerationPort {
    CompletableFuture<GeneratedCoverLetterDto> generate(
            GenerateCoverLetterCommand command
    );
}
```

Для вопросов:

```java
public interface EmployerQuestionAnswerGenerationPort {
    CompletableFuture<List<EmployerQuestionAnswerDto>> generateAnswers(
            AnswerEmployerQuestionCommand command
    );
}
```

### 30.3. Use cases

```text
GenerateCoverLetterUseCase
GenerateEmployerQuestionAnswersUseCase
```

### 30.4. Infrastructure adapter

Варианты adapter-ов:

```text
OpenAiCoverLetterGenerationAdapter
OllamaCoverLetterGenerationAdapter
LocalLlmHttpCoverLetterGenerationAdapter
```

На первом этапе лучше:

```text
LocalLlmHttpCoverLetterGenerationAdapter
```

Потому что он не привязывает проект к конкретной модели.

---

## 31. Интеграция LLM в текущий AutoResponses flow

Текущий flow:

```text
Вакансия из API -> очередь -> READY -> browser-agent -> шаблонное письмо
```

Будущий flow:

```text
Вакансия из API
-> очередь
-> READY
-> GenerateCoverLetterUseCase
-> BrowserAgent получает сгенерированное письмо
-> отклик/чат
-> статус SENT
```

Для вопросов работодателя:

```text
BrowserAgent обнаружил questionnaire
-> считал вопросы
-> GenerateEmployerQuestionAnswersUseCase
-> заполнил ответы
-> отправил или попросил подтверждение
```

Важно:

```text
Вопросы работодателя лучше сначала делать с ручным подтверждением.
Автоматически отправлять ответы на вопросы только после отдельного режима и тестов.
```

---

## 32. Что нужно попросить у пользователя в следующем чате

Для LLM-этапа попросить актуальные файлы:

```text
AutoResponseExecutionRequest.java
AutoResponseExecutionResultDto.java
AutoResponseExecutionService.java
HhAutoResponseExecutionAdapter.java
HhBrowserAutoResponseAgent.java
HhBrowserAutoResponseProperties.java
AutoResponseQueueItem.java
AutoResponseQueueService.java
AutoResponsesController.java
auto-responses.fxml
HhVacancyDto.java
HhVacancySearchService.java
HhApiClient.java
application.yaml
pom.xml root
infrastructure/pom.xml
```

Если нужно хранить generated letters:

```text
AutoResponseQueueItemEntity.java
AutoResponseQueueItem.java
AutoResponseQueueRepository.java
AutoResponseQueueRepositoryAdapter.java
SpringDataAutoResponseQueueItemJpaRepository.java
Flyway migrations
```

Если нужно подтягивать профиль/резюме пользователя:

```text
ProfileController.java
profile.fxml
AuthenticatedUserDto.java
CurrentUserSession.java
User domain/entity/repository files
```

---

## 33. Важные текущие env variables

### HH API/OAuth

```env
HH_CLIENT_ID=...
HH_CLIENT_SECRET=...
HH_REDIRECT_MODE=CUSTOM_URI_SCHEME
HH_REDIRECT_URI=jobhunterpro://oauth/hh/callback
HH_CUSTOM_SCHEME_FORWARD_PORT=54347
HH_API_BASE_URL=https://api.hh.ru
HH_USER_AGENT=JobHunterPro/0.1.0 (...)
HH_APPLICATION_ACCESS_TOKEN=...
```

### HH browser-agent

```env
HH_BROWSER_AUTO_RESPONSE_ENABLED=true
HH_BROWSER_AUTO_RESPONSE_DRY_RUN=false
HH_BROWSER_AUTO_RESPONSE_HEADLESS=false
HH_BROWSER_USER_DATA_DIR=C:\Users\beats\.jobhunterpro\hh-chrome-profile
HH_BROWSER_WAIT_TIMEOUT_SECONDS=10
HH_BROWSER_DEFAULT_MESSAGE=Здравствуйте! Откликаюсь на вакансию "{vacancyName}". Буду рад обсудить детали.
```

### Habr Career

Habr Career infrastructure реализована, но real-flow заблокирован:

```env
HABR_CAREER_API_BASE_URL=https://career.habr.com/api
HABR_CAREER_API_ALLOW_NON_HABR_CAREER_BASE_URL=false
HABR_CAREER_OAUTH_AUTHORIZATION_URL=https://career.habr.com/integrations/oauth/authorize
HABR_CAREER_OAUTH_TOKEN_URL=https://career.habr.com/integrations/oauth/token
HABR_CAREER_CLIENT_ID=...
HABR_CAREER_CLIENT_SECRET=...
HABR_CAREER_REDIRECT_URI=jobhunterpro://oauth/habr/callback
HABR_CAREER_OAUTH_ALLOW_NON_HABR_CAREER_URLS=false
HABR_CAREER_OAUTH_STATE_BYTE_LENGTH=32
HABR_CAREER_USER_AGENT=JobHunterPro/0.1.0 (...)
```

Важно:

```text
.env не подхватывается автоматически IntelliJ/Spring Boot.
Переменные задавались через IntelliJ Run Configuration -> Environment variables.
Не коммитить реальные secrets/tokens.
```

---

## 34. Важные команды

### 34.1. Тесты

```bash
mvn clean test
```

Если упал конкретный module:

```bash
mvn <args> -rf :infrastructure
```

### 34.2. Очередь автооткликов

```sql
SELECT id,
       user_id,
       source,
       external_vacancy_id,
       vacancy_name,
       employer_name,
       area_name,
       status,
       created_at,
       updated_at
FROM auto_response_queue_items
ORDER BY created_at DESC;
```

### 34.3. Вернуть элемент в READY для dev-теста

```sql
UPDATE auto_response_queue_items
SET status = 'READY',
    updated_at = now()
WHERE id = '<queue_item_id>'
  AND user_id = '<user_id>';
```

### 34.4. Запуск Chrome с Selenium-профилем

```powershell
$chromeCandidates = @(
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
  "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
  "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
)

$chrome = $chromeCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

& $chrome `
  --user-data-dir="$env:USERPROFILE\.jobhunterpro\hh-chrome-profile" `
  --profile-directory=Default `
  "https://hh.ru"
```

Альтернатива:

```powershell
Start-Process chrome.exe -ArgumentList @(
  "--user-data-dir=$env:USERPROFILE\.jobhunterpro\hh-chrome-profile",
  "--profile-directory=Default",
  "https://hh.ru"
)
```

### 34.5. Debug HTML/Screenshot

Browser-agent сохраняет диагностику в:

```text
logs/hh-browser-debug
```

Файлы вида:

```text
execution-error-....html
execution-error-....png
dry-run-cover-letter-inserted-....html
dry-run-cover-letter-inserted-....png
response-sent-....html
response-sent-....png
question-form-detected-....html
question-form-detected-....png
```

---

## 35. Важные правила безопасности

Нельзя логировать и нельзя включать в отчёты:

```text
client_secret
access_token
refresh_token
authorization_code
полный callback URI с code/state
реальные значения env credentials
cookie/session данные Chrome-профиля
```

Можно логировать:

```text
userId
provider
status
source
externalVacancyId
vacancyId
queueItemId
факт успешного подключения
факт успешной проверки API
факт успешного browser-flow
```

Важно:

```text
Пользователь показывал HH Client Secret в чате/скриншоте.
После завершения отладки рекомендуется перевыпустить secret в HH developer cabinet.
```

---

## 36. Что получилось

```text
HH API:
- OAuth пользователя работает;
- /me работает;
- application token получен;
- поиск вакансий работает через application token;
- official auto-apply API заблокирован 403 forbidden.

AutoResponses:
- очередь работает;
- статусы работают;
- READY-фильтр работает;
- execution pipeline работает;
- browser execution adapter работает для базового сценария.

Browser-agent:
- Selenium подключён;
- persistent Chrome profile настроен;
- HH web-сессия авторизована;
- anonymous-сессия обнаруживается и обрабатывается;
- кнопка "Откликнуться" нажимается;
- success state "Отклик отправлен" / "Резюме доставлено" определяется;
- сопроводительное письмо вставляется/отправляется в поддерживаемом сценарии;
- diagnostics screenshot/html сохраняются.

Ограничения:
- вакансии с вопросами работодателя пока не поддержаны;
- LLM ещё не подключена;
- сопроводительное письмо пока шаблонное.
```

---

## 37. Что не получилось или отложено

### 37.1. Official HH API auto-response

Не получилось из-за внешнего ограничения HH:

```text
GET /resumes/mine -> 403 forbidden
POST /negotiations -> 403 forbidden
```

Решение:

```text
Использовать HH API для поиска вакансий.
Отправку делать browser-agent-ом.
```

### 37.2. Вакансии с вопросами

Не реализовано:

```text
QuestionnaireHandler
LLM-generated answers
manual review of answers
auto-fill question forms
```

### 37.3. LLM

Пока не реализовано:

```text
LLM provider
LLM prompt templates
cover letter generation
question-answer generation
integration with browser-agent
```

---

## 38. Текущая точка остановки

Остановились здесь:

```text
2.4 HH Browser Auto Response Agent — базовый сценарий работает.
```

Что подтверждено:

```text
Пользователь авторизовал Selenium Chrome profile.
Агент успешно проходит основной сценарий.
HH показывает "Отклик отправлен" / "Резюме доставлено".
Пользователь написал: "Сейчас все окей проходит".
```

Почему останавливаемся:

```text
Дальше развитие автооткликов упирается в:
- генерацию качественного сопроводительного письма;
- ответы на вопросы работодателя;
- обработку нестандартных форм.

Все эти задачи лучше решать через LLM.
```

---

## 39. Следующая задача в новом чате

```text
Подключить LLM-модель, протестировать её отдельно и затем интегрировать в JobHunterPro.
```

Рекомендуемый порядок:

```text
1. Выбрать способ LLM:
   - local Ollama/LM Studio;
   - локальный HTTP microservice;
   - облачный API.

2. Сделать отдельный тест генерации сопроводительного письма:
   input:
   - vacancyName;
   - employerName;
   - vacancyDescription/requirements;
   - userProfile/resumeSummary.

   output:
   - короткое сопроводительное письмо на русском;
   - без воды;
   - без выдуманного опыта;
   - тон: вежливо, уверенно, junior/middle depending on profile.

3. Сделать core contracts:
   - GenerateCoverLetterUseCase;
   - CoverLetterGenerationPort;
   - GenerateCoverLetterCommand;
   - GeneratedCoverLetterDto.

4. Реализовать infrastructure adapter:
   - LocalLlmHttpCoverLetterGenerationAdapter или Ollama adapter.

5. Интегрировать в HhAutoResponseExecutionAdapter:
   - перед browser-agent генерировать письмо;
   - browser-agent получает уже готовый текст.

6. Позже добавить:
   - EmployerQuestionExtractor;
   - GenerateEmployerQuestionAnswersUseCase;
   - QuestionnaireHandler.
```

---

## 40. Prompt для следующего чата

```text
Продолжаем разработку JobHunterPro Desktop Application.

Прочитай приложенный markdown-отчёт.
Не гадай по отсутствующим файлам: если нужен актуальный код, попроси приложить файл.

Текущее состояние:
- HH.ru OAuth работает.
- HH API /me работает.
- HH application token работает.
- Поиск вакансий через HH API работает.
- Официальный HH API для /resumes/mine и POST /negotiations возвращает 403 forbidden.
- Поэтому отправка откликов реализована через Selenium browser-agent.
- Selenium Chrome profile авторизован в HH.ru.
- Базовый browser-flow автоотклика работает: агент открывает вакансию, нажимает "Откликнуться", определяет success state и отправляет/вставляет сопроводительное письмо.
- Вакансии с вопросами работодателя пока не поддержаны.
- Сопроводительное письмо пока шаблонное.

Мы решили остановиться на автооткликах и перейти к следующему этапу:
подключение LLM-модели, её тестирование и интеграция в приложение.

Главная задача:
спроектировать и реализовать LLM-интеграцию для генерации сопроводительного письма.
Следом — подготовить основу для генерации ответов на вопросы работодателя.

Начни с выбора подхода:
- локальная модель через Ollama/LM Studio;
- локальный HTTP microservice;
- облачный API.

Потом попроси у пользователя актуальные файлы:
AutoResponseExecutionRequest.java
AutoResponseExecutionResultDto.java
AutoResponseExecutionService.java
HhAutoResponseExecutionAdapter.java
HhBrowserAutoResponseAgent.java
HhBrowserAutoResponseProperties.java
AutoResponseQueueItem.java
AutoResponseQueueService.java
AutoResponsesController.java
auto-responses.fxml
HhVacancyDto.java
HhVacancySearchService.java
HhApiClient.java
application.yaml
root pom.xml
infrastructure/pom.xml
```

---

## 41. Самый короткий итог

```text
В этом чате:
- проверили официальный HH API для отклика;
- выяснили, что /resumes/mine и POST /negotiations закрыты 403 forbidden;
- исправили поиск вакансий через HH application token;
- решили использовать API только для поиска вакансий;
- реализовали Selenium browser-agent для отправки откликов;
- настроили persistent Chrome profile;
- авторизовали HH web-сессию в Selenium-профиле;
- довели базовый browser-flow до успешного отклика;
- выявили отдельный сценарий вакансий с вопросами;
- решили остановиться на автооткликах и перейти к LLM.

Дальше:
подключить LLM-модель для генерации сопроводительных писем и будущих ответов на вопросы работодателя.
```
