# HH.ru OAuth callback flow в JobHunterPro

## 1. Назначение документа

Этот документ описывает, как в JobHunterPro устроена обработка OAuth callback для интеграции с HH.ru.

На текущем этапе проект поддерживает два режима redirect:

```text
LOCAL_HTTP_SERVER
CUSTOM_URI_SCHEME
```

Режим `LOCAL_HTTP_SERVER` используется для локальной разработки через HTTP callback server.

Режим `CUSTOM_URI_SCHEME` подготовлен для desktop-сценария через URI вида:

```text
jobhunterpro://oauth/hh/callback
```

Этот режим соответствует redirect URI, указанному в заявке приложения JobHunterPro на стороне HH.ru.

---

## 2. Текущее состояние HH.ru заявки

Заявка приложения JobHunterPro в HH.ru находится на рассмотрении.

Поэтому этап реальной проверки OAuth-flow с настоящими `client_id` и `client_secret` пока отложен.

Статус:

```text
2.1.8 — real HH OAuth-flow check with credentials — DEFERRED
```

Причина:

```text
HH.ru application is still under review.
Credentials are not available yet.
```

Текущий redirect URI в заявке HH.ru:

```text
jobhunterpro://oauth/hh/callback
```

---

## 3. Конфигурация

Основная конфигурация находится в:

```text
app/src/main/resources/application.yaml
```

Правильный путь свойств:

```yaml
jobhunter:
  integrations:
    hh:
      oauth:
        authorization-url: https://hh.ru/oauth/authorize
        token-url: https://api.hh.ru/token
        client-id: ${HH_CLIENT_ID:}
        client-secret: ${HH_CLIENT_SECRET:}
        redirect-uri: ${HH_REDIRECT_URI:http://127.0.0.1:54345/oauth/hh/callback}
        redirect-mode: ${HH_REDIRECT_MODE:LOCAL_HTTP_SERVER}
        callback-port: ${HH_CALLBACK_PORT:54345}
        state-byte-length: ${HH_OAUTH_STATE_BYTE_LENGTH:32}
        user-agent: ${HH_USER_AGENT:JobHunterPro/0.1.0 (beatsluffi@gmail.com)}
```

Важно: путь должен быть именно таким:

```text
jobhunter.integrations.hh.oauth
```

Нельзя делать лишний уровень:

```text
jobhunter.integrations.integrations.hh.oauth
```

Иначе `HhOAuthProperties` не получит значения из YAML, а redirect mode останется дефолтным.

---

## 4. HhOAuthProperties

Файл:

```text
infrastructure/src/main/java/ru/jobhunter/infrastructure/platform/hh/auth/HhOAuthProperties.java
```

Класс биндингует настройки по prefix:

```java
@ConfigurationProperties(prefix = "jobhunter.integrations.hh.oauth")
public record HhOAuthProperties(
        String authorizationUrl,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        String redirectMode,
        int callbackPort,
        int stateByteLength,
        String userAgent
) {

    public HhOAuthRedirectMode parsedRedirectMode() {
        return HhOAuthRedirectMode.from(redirectMode);
    }
}
```

---

## 5. Режим LOCAL_HTTP_SERVER

Этот режим используется для разработки через локальный HTTP callback server.

Пример конфигурации:

```env
HH_REDIRECT_MODE=LOCAL_HTTP_SERVER
HH_REDIRECT_URI=http://127.0.0.1:54345/oauth/hh/callback
HH_CALLBACK_PORT=54345
```

Сценарий:

```text
1. Пользователь нажимает "Подключить HH.ru".
2. Приложение генерирует authorization URL.
3. Приложение поднимает локальный HTTP server.
4. Браузер открывает страницу авторизации HH.ru.
5. HH.ru делает redirect на http://127.0.0.1:54345/oauth/hh/callback.
6. Приложение получает code и state.
7. Приложение меняет authorization code на tokens.
8. Tokens сохраняются в PostgreSQL.
```

Класс, который отвечает за локальный callback:

```text
HhOAuthCallbackServer
```

Он является стратегией:

```text
HhOAuthCallbackStrategy
```

и работает только для режима:

```text
LOCAL_HTTP_SERVER
```

---

## 6. Режим CUSTOM_URI_SCHEME

Этот режим нужен для desktop redirect через custom protocol:

```text
jobhunterpro://oauth/hh/callback
```

Пример конфигурации:

```env
HH_REDIRECT_MODE=CUSTOM_URI_SCHEME
HH_REDIRECT_URI=jobhunterpro://oauth/hh/callback
HH_CUSTOM_SCHEME_FORWARD_PORT=54347
```

В IntelliJ удобнее задавать через VM options:

```text
-Djobhunter.integrations.hh.oauth.redirect-mode=CUSTOM_URI_SCHEME
-Djobhunter.integrations.hh.oauth.redirect-uri=jobhunterpro://oauth/hh/callback
-Djobhunter.hh.custom-scheme-forward-port=54347
```

---

## 7. Архитектура callback strategy

Основная цепочка:

```text
ProfileController
    ↓
ConnectHhAccountUseCase
    ↓
HhOAuthConnectionService
    ↓
HhOAuthCallbackWaiter
    ↓
HhOAuthCallbackWaiterSelector
    ↓
HhOAuthCallbackStrategy
```

`HhOAuthConnectionService` не знает, какой конкретно redirect mode используется.

Он зависит только от интерфейса:

```text
HhOAuthCallbackWaiter
```

Выбор конкретной стратегии делает:

```text
HhOAuthCallbackWaiterSelector
```

Стратегии:

```text
LOCAL_HTTP_SERVER  → HhOAuthCallbackServer
CUSTOM_URI_SCHEME → HhOAuthCustomSchemeCallbackWaiter
```

---

## 8. CUSTOM_URI_SCHEME forwarding flow

Проблема desktop-приложения:

```text
Когда браузер открывает jobhunterpro://..., Windows запускает новый процесс приложения.
Но OAuth CompletableFuture ожидает callback в уже запущенном процессе.
```

Для решения добавлен forwarding mechanism.

Сценарий:

```text
1. Основное приложение запущено.
2. В режиме CUSTOM_URI_SCHEME оно поднимает forward-server на 127.0.0.1:54347.
3. Браузер открывает jobhunterpro://oauth/hh/callback?code=...&state=...
4. Windows вызывает protocol handler.
5. Protocol handler запускает PowerShell script.
6. Script отправляет callback URI в уже запущенное приложение через 127.0.0.1:54347.
7. Forward-server принимает URI.
8. Dispatcher передает URI в callback registry.
9. Registry завершает ожидающий CompletableFuture, если state совпадает.
```

Основные классы:

```text
HhOAuthCustomSchemeForwardServer
HhOAuthCustomSchemeForwardClient
HhOAuthCustomSchemeCallbackDispatcher
HhOAuthCustomSchemeCallbackRegistry
HhOAuthCustomSchemeCallbackWaiter
HhOAuthCustomSchemeArgumentDetector
HhOAuthCustomSchemeForwardingConfig
```

---

## 9. Windows protocol handler scripts

Скрипты находятся здесь:

```text
scripts/windows
```

Файлы:

```text
register-jobhunterpro-protocol-dev.ps1
unregister-jobhunterpro-protocol-dev.ps1
forward-hh-oauth-callback.ps1
```

Регистрация protocol handler:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\windows\register-jobhunterpro-protocol-dev.ps1
```

Ожидаемый вывод:

```text
JobHunterPro protocol handler registered for current user.
Protocol: jobhunterpro://
Command: powershell.exe -NoProfile -ExecutionPolicy Bypass -File "...forward-hh-oauth-callback.ps1" "%1"
```

Удаление protocol handler:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\windows\unregister-jobhunterpro-protocol-dev.ps1
```

Регистрация выполняется в:

```text
HKCU\Software\Classes\jobhunterpro
```

Администраторские права не нужны.

---

## 10. Проверка CUSTOM_URI_SCHEME forwarding

Запустить приложение с настройками:

```text
-Djobhunter.integrations.hh.oauth.redirect-mode=CUSTOM_URI_SCHEME
-Djobhunter.integrations.hh.oauth.redirect-uri=jobhunterpro://oauth/hh/callback
-Djobhunter.hh.custom-scheme-forward-port=54347
```

После старта должен появиться лог:

```text
HH OAuth custom URI forward server started: port=54347
```

Проверка порта:

```powershell
netstat -ano | findstr :54347
```

Ожидаемо:

```text
TCP    127.0.0.1:54347    0.0.0.0:0    LISTENING
```

Ручная проверка callback:

```powershell
start "jobhunterpro://oauth/hh/callback?code=test-code&state=test-state"
```

Если OAuth-flow с таким state не был запущен, ожидаемый лог:

```text
HH OAuth custom URI forwarded callbacks processed: count=0
```

Это нормальный результат. Он означает, что:

```text
Windows protocol handler работает.
PowerShell forwarding script работает.
Java forward-server работает.
Dispatcher получил URI.
Но pending state не найден.
```

Полное завершение callback будет возможно только во время настоящего OAuth-flow, когда приложение заранее зарегистрирует ожидаемый state.

---

## 11. Безопасность

Нельзя логировать:

```text
HH_CLIENT_SECRET
access_token
refresh_token
authorization code
full callback URI
```

Callback URI может содержать authorization code:

```text
jobhunterpro://oauth/hh/callback?code=...&state=...
```

Поэтому в логах нужно писать только технические статусы без полного URI.

Допустимо логировать:

```text
redirect mode
provider
userId
status
port
count processed callbacks
```

---

## 12. Проверка токенов в PostgreSQL

После успешного OAuth-flow токены сохраняются в таблицу:

```text
external_auth_tokens
```

Безопасный SQL для проверки:

```sql
select
    id,
    user_id,
    provider,
    token_type,
    scope,
    expires_at,
    created_at,
    updated_at
from external_auth_tokens
order by updated_at desc;
```

Нельзя выполнять:

```sql
select * from external_auth_tokens;
```

потому что таблица содержит:

```text
access_token
refresh_token
```

---

## 13. Тесты

Добавлены или должны быть добавлены тесты для custom scheme callback infrastructure:

```text
HhOAuthCustomSchemeCallbackRegistryTest
HhOAuthCustomSchemeCallbackDispatcherTest
```

Проверяемые сценарии:

```text
Registry завершает callback при совпадающем state.
Registry возвращает false при неизвестном state.
Registry завершает future ошибкой при timeout.
Registry обрабатывает callback с error.
Registry отклоняет URI с неправильным scheme.
Registry отклоняет URI без code.
Registry отклоняет URI без state.
Dispatcher игнорирует обычные аргументы.
Dispatcher обрабатывает jobhunterpro:// callback URI.
Dispatcher считает только поддерживаемые аргументы.
```

Команда проверки:

```bash
mvn clean test
```

Ожидаемый результат:

```text
BUILD SUCCESS
```

---

## 14. Текущий статус

Готово:

```text
2.1.6 UI-кнопка "Подключить HH.ru"
2.1.7 Статус подключения HH.ru в профиле
2.1.9 Redirect strategy для LOCAL_HTTP_SERVER и CUSTOM_URI_SCHEME
2.1.9.10 Windows dev protocol handler forwarding
2.1.9.11 Custom scheme callback tests
```

Отложено:

```text
2.1.8 Real HH OAuth-flow check with credentials
```

Причина:

```text
HH.ru application is still under review.
```

Следующий технический шаг после одобрения заявки:

```text
1. Получить HH_CLIENT_ID и HH_CLIENT_SECRET.
2. Запустить приложение в CUSTOM_URI_SCHEME mode.
3. Нажать "Подключить HH.ru".
4. Проверить redirect через jobhunterpro://.
5. Проверить сохранение токенов в external_auth_tokens.
6. Проверить access token через HH API /me.
```

---

## 15. Возможные проблемы

### Forward-server не стартует

Проверить:

```text
1. redirect-mode действительно CUSTOM_URI_SCHEME.
2. В YAML нет лишнего уровня integrations.
3. HhOAuthCustomSchemeForwardServer имеет @Component.
4. Класс находится внутри scanBasePackages = "ru.jobhunter".
5. В логах есть строка:
   HH OAuth custom URI forward server started: port=54347
```

### netstat не показывает порт 54347

Значит forward-server не поднялся.

Команда:

```powershell
netstat -ano | findstr :54347
```

Ожидаемо:

```text
LISTENING
```

### PowerShell script запускается, но callback не доходит

Проверить:

```text
1. Protocol handler зарегистрирован.
2. forward-hh-oauth-callback.ps1 существует.
3. Порт 54347 слушается.
4. HH_CUSTOM_SCHEME_FORWARD_PORT совпадает с портом Java forward-server.
```

### processed callbacks count=0

Это нормально для ручного теста с произвольным state.

Причина:

```text
В registry нет pending callback с таким state.
```

Во время настоящего OAuth-flow state будет создан приложением перед открытием браузера.

---

## 16. Важное замечание для production

Dev-скрипты PowerShell подходят для разработки.

Для production-сборки через `jpackage` регистрацию protocol handler нужно переносить в installer / post-install script.

Также desktop-приложение не является безопасным местом для хранения `client_secret`.

В будущем желательно перейти на один из вариантов:

```text
OAuth PKCE flow
или
backend proxy для token exchange
```
