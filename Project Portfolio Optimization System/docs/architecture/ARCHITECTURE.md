# Architecture — Project Portfolio Optimization System

**Статус:** `proposed` → ожидает утверждения студентом.  
**Дата:** 2026-11-14.  
**Основа:** `docs/planning/PROJECT-BRIEF.md`, `docs/planning/architecture-options.md`.

**Принятые решения (2026-11-14):**

| ID | Решение |
|----|---------|
| ARCH-01 | 5 backend-процессов (gateway, core, calc, finance, fts) + SPA; без Kafka, K8s |
| ARCH-02 | Core — модульный монолит на **Spring Modulith**, 7 модулей (пакеты + verify-тест границ + доменные события) |
| ARCH-03 | Finance — **stateless** (парсинг + NPV); импорты и снимки хранит core |
| ARCH-04 | Async-оптимизация — job в core; calc синхронный и stateless |
| ARCH-05 | Сервисы собираются «как конструктор»: Spring Boot + существующие `libs/` + контракт из `contracts/openapi/`; новых библиотек заранее не заводим |
| ARCH-06 | **FTS** (file transfer service) — единственная обёртка над MinIO; остальные сервисы работают с `fileId` |
| ARCH-07 | **Spring Cloud Gateway** — единая точка входа: маршрутизация, CORS, rate-limit, ранняя проверка JWT |

---

## 1. Компоненты

```text
browser ──► nginx :80 (статика SPA; /api/** → gateway)
                 │
                 ▼
          gateway :8090 (Spring Cloud Gateway)
           └─ /api/**       ──► core :8080 ─── PG(schema core)
                                 ├─► calc    :8081  stateless
                                 ├─► finance :8082  stateless ──► fts
                                 └─► fts     :8083 ──── MinIO + PG(schema fts)
                                     (только docker-сеть: core, finance)
```

| Компонент | Ответственность | Не делает |
|-----------|-----------------|-----------|
| **frontend** | React + TS SPA; nginx раздаёт статику и проксирует `/api` в gateway | Бизнес-логику |
| **gateway** | Маршруты по префиксу, CORS, rate-limit на `POST /api/v1/auth/login`, проверка JWT (кроме `/api/v1/auth/*`) | Бизнес-логику, выдачу токенов |
| **core** | Auth (выдача JWT), пользователи/роли, справочники, проекты, критерии/оценки (хранение), импорты/снимки (хранение), сценарии, запуски оптимизации, утверждение, аудит, отчёты, уведомления | SAW, knapsack, парсинг CSV/XLSX, хранение байтов файлов |
| **calc** | SAW (нормализация + вклад критериев), бинарная оптимизация 0-1 | Хранение, пользователей |
| **finance** | Парсинг/валидация CSV/XLSX (файл берёт из FTS по `fileId`) → строки + протокол ошибок; расчёт NPV | Хранение снимков |
| **fts** | Внутренний: загрузка/выдача/метаданные/soft-delete; sha256; presigned URL | Доменную логику, разбор содержимого |

Безопасность: gateway проверяет JWT рано, но каждый сервис **всё равно** валидирует его через `common-security`
(defense in depth; сервис не доверяет сети). Межсервисные вызовы передают токен пользователя. Наружу открыт
только nginx; gateway, сервисы, MinIO и PostgreSQL — только в docker-сети.

### FTS

- Внутренний сервис: браузер в FTS не ходит, gateway не маршрутизирует `/api/files/**`.
  Доступен только в docker-сети для core и finance.
- Загрузка: browser → core → FTS. Core проверяет права на доменный объект (импорт, отчёт, вложение)
  и вызывает FTS; браузер скачивает по presigned URL, который core запрашивает у FTS после проверки прав.
- Метаданные — таблица `files` (id, bucket, object_key, original_name, size, content_type, sha256, owner_id,
  owner_service, created_by, created_at, deleted_at) в схеме `fts`; байты — в MinIO.
- Бакеты по назначению: `imports`, `reports`, `attachments`. Ключ объекта генерирует FTS (UUID), имя файла из запроса — только метаданные.
- Лимиты: размер и allow-list content-type на бакет (конфиг). Сигнатура первых байт сверяется с заявленным типом (без Tika):
  PDF `%PDF-`, PNG `89 50 4E 47 0D 0A 1A 0A`, JPEG `FF D8 FF`, OOXML (xlsx/docx) `PK 03 04`;
  `text/csv`/`text/plain` — первые 8 КБ без байта `00` и валидный UTF-8. Несовпадение → 415 `CONTENT_MISMATCH`.
- Файл неизменяем: новая версия = новый `fileId`. Снимок финансов ссылается на `fileId` + sha256 → воспроизводимость.
- Доступ внутри FTS: читать — любой валидный JWT (core/finance с токеном пользователя); удалять — владелец (`owner_id`) или `ADMIN`.

---

## 2. «Конструктор»: как собирается сервис

Сервис = Spring Boot + `common-security` (+ `common-web`, если есть CRUD) + свой OpenAPI-контракт.

```text
libs/
  common-security   JWT validate-only, 401/403 Problem Details, method security, AuditorAware
  common-web        BaseEntity, AbstractJpaCrud*, RSQL allow-list

contracts/openapi/
  calc-v1.yaml, finance-v1.yaml, fts-v1.yaml, core-v1.yaml

services/gateway/   Spring Cloud Gateway (конфиг маршрутов + фильтры)

services/<svc>/
  <svc>-api    DTO + интерфейсы из contracts/openapi/<svc>-v1.yaml
  <svc>-impl   Spring Boot app
```

Правила:

1. Contract-first: сначала правится `contracts/openapi/*.yaml`, затем код.
2. Клиенты к calc/finance/fts генерируются openapi-generator'ом из контракта **в сервисе-потребителе**; отдельных client-библиотек нет.
3. Новая lib в `libs/` появляется только когда один и тот же код реально нужен второму сервису; до этого он живёт в сервисе.
4. Каждый сервис — отдельный Dockerfile, все собираются в `infra/docker-compose.yml`.

**План отката:** при нехватке времени `finance-impl` подключается в core как библиотека, а адаптер `FinanceClient`
переключается с HTTP на локальный вызов (`portfolio.finance.mode=embedded|remote`). Контракт не меняется.
Gateway при необходимости заменяется nginx-маршрутами (`/api/**` → core) без изменений в сервисах.
Calc и FTS остаются отдельными процессами → требование «≥2 компонента» сохраняется.

---

## 3. Core: модули (Spring Modulith)

```text
com.portfolio.core
  shared/      общие типы, Money/BigDecimal-утилиты, @Audited (не модуль, общий код)
  identity/    ✔ пользователи, роли, блокировка + auth/ (login/refresh/logout, выдача JWT) как подпакет
  reference/   подразделения, бюджетные периоды, стратегические цели, валюта (soft-delete при ссылках)
  project/     карточка, lifecycle, связи (зависимость/взаимоисключение), поиск/фильтр/paging, вложения (fileId)
  scoring/     критерии, версии модели + веса (Σ=1), экспертные оценки → CalcClient.saw
  finance/     сессии импорта (fileId + sha256), ошибки, публикация снимка, денежные потоки → FtsClient, FinanceClient
  portfolio/   сценарии, кандидаты/ограничения, optimization_run (job), постпроверка, утверждение, отчёт (→ FTS)
  activity/    слушатели доменных событий → audit_log + notifications (in-app, email stub); сбой доставки не откатывает операцию
```

Итого 7 модулей. Текущие пакеты `auth/` и `identity/` сливаются в модуль `identity` (рефакторинг перемещением, без изменения логики).

Правила:

- Модуль публикует API только в корневом пакете модуля; `internal/` недоступен другим (проверяет `ApplicationModules.verify()`).
- Межмодульные побочные эффекты — через доменные события (`@ApplicationModuleListener`), без прямых вызовов:
  `ProjectSubmitted`, `AssessmentCompleted`, `SnapshotPublished`, `ModelWeightsChanged`, `RunCompleted`, `ScenarioApproved`.
- Внешние вызовы — только через клиент-интерфейсы (`CalcClient`, `FinanceClient`, `FtsClient`) в модуле-потребителе.

---

## 4. Контракты (черновик ресурсов)

### core → calc (`calc-v1`)

| Метод | Ресурс | Вход → выход |
|-------|--------|--------------|
| POST | `/v1/scoring/saw` | критерии (вес, направление min/max), матрица значений → баллы + вклад каждого критерия; ошибка при пропуске значения (без silent zero) |
| POST | `/v1/optimization/binary` | проекты (балл, стоимость), бюджет, mandatory/forbidden, зависимости/исключения → выбранный набор, сумма баллов, бюджет, статус `OPTIMAL/INFEASIBLE` |

### core → finance (`finance-v1`)

| Метод | Ресурс | Вход → выход |
|-------|--------|--------------|
| POST | `/v1/imports/parse` | `{fileId}` → finance скачивает файл из FTS → нормализованные строки потоков + список ошибок (строка, колонка, код) + sha256 |
| POST | `/v1/indicators/npv` | потоки по периодам, ставка → NPV по проектам |

### core, finance → fts (`fts-v1`)

| Метод | Ресурс | Вход → выход |
|-------|--------|--------------|
| POST | `/v1/files` (multipart, `bucket`) | файл → `{fileId, sha256, size, contentType}` |
| GET | `/v1/files/{id}` | метаданные |
| GET | `/v1/files/{id}/content` | поток байтов (для сервисов) |
| POST | `/v1/files/{id}/download-url` | presigned URL с коротким TTL (для браузера) |
| DELETE | `/v1/files/{id}` | soft-delete |

### gateway (маршруты)

| Префикс | Куда | JWT на gateway |
|---------|------|----------------|
| `/api/v1/auth/**` | core | нет (rate-limit на `POST /api/v1/auth/login`) |
| `/api/**` | core | да |
| `/api/v1/files/**` | — (404, внутренний FTS) | — |

### frontend → core (`core-v1`, основные группы)

`/auth/*` ✔ · `/users/*` ✔ · `/reference/*` · `/projects` (+ `/{id}/status`, `/{id}/links`) ·
`/criteria`, `/scoring-models` (+ `/{id}/weights`), `/projects/{id}/assessments`, `/scoring-models/{id}/scores` ·
`/finance/imports` (+ `/{id}/errors`, `/{id}/publish`), `/finance/snapshots` ·
`/scenarios` (+ `/{id}/candidates`, `/{id}/runs` → `202 {jobId}`), `/runs/{id}` (статус/результат/объяснение),
`/scenarios/{id}/approve` · `/reports/runs/{id}` · `/notifications` · `/audit` (ADMIN).

Общие правила: версия в пути, ошибки — RFC 9457 Problem Details, таймауты на клиенте, деньги/веса/баллы —
строки-десятичные (`BigDecimal`), ошибка calc/finance → явная ошибка пользователю (не пустой портфель).

---

## 5. Сквозной сценарий

```text
1. Аналитик загружает XLSX → gateway → core.finance → FTS (fileId, sha256)
   → finance/parse {fileId} → finance читает файл из FTS → import_session + import_errors
2. Публикация → неизменяемый financial_snapshot + cash_flows → finance/npv → значение критерия «NPV»
3. Эксперты вводят оценки; модель весов зафиксирована → calc/saw → баллы + вклад
4. Сценарий: бюджет, кандидаты, mandatory/forbidden → POST /runs → 202 jobId
   core @Async: снимок входов → calc/binary → постпроверка ограничений → RunCompleted
5. ЛПР сравнивает запуски, выбирает альтернативу → approve → ScenarioApproved → activity (audit + notification)
   → отчёт по запуску генерируется и сохраняется в FTS (bucket reports) → ссылка через presigned URL
6. Изменился снимок или веса → связанные runs помечаются STALE (BR-10/23)
```

---

## 6. Логическая модель данных (core ~24 таблицы + fts 1)

| Модуль | Таблицы |
|--------|---------|
| identity | `users` ✔, `refresh_sessions` ✔ |
| reference | `departments`, `budget_periods`, `strategic_goals` |
| project | `projects`, `project_goals`, `project_links`, `project_attachments` (fileId) |
| scoring | `criteria`, `scoring_models`, `model_weights`, `assessments`, `project_scores` |
| finance | `import_sessions` (fileId, sha256), `import_errors`, `financial_snapshots`, `cash_flows` |
| portfolio | `scenarios`, `scenario_items`, `optimization_runs`, `run_items`, `approvals`, `reports` (fileId) |
| activity | `audit_log`, `notifications` |
| **fts** (схема `fts`) | `files` (+ `owner_id`) |

Каждый модуль владеет своими таблицами и Flyway-миграциями (префикс версии по модулю); чужие таблицы читаются только через API модуля.
Один экземпляр PostgreSQL, отдельные схемы `core` и `fts` с отдельными пользователями БД. Ссылки на `fileId` между схемами — без FK.

---

## 7. Роли

| Роль в коде | Предметные роли КП |
|-------------|--------------------|
| `INITIATOR` | Инициатор, Руководитель проекта |
| `EXPERT_ANALYST` | Эксперт, Финансовый аналитик, Менеджер портфеля |
| `DECISION_MAKER` | Член комитета (ЛПР) |
| `ADMIN` | Системный администратор |

---

## 8. Порядок реализации

| Wave | Содержание | Закрывает |
|------|------------|-----------|
| W1 | Контракты calc/finance/fts, gateway, docker-compose (+ MinIO), Modulith в core, слияние `auth`→`identity` | инфраструктура, REQ-INT |
| W2 | fts; calc: SAW + 0-1 (своя ДП, n≈30); параллельно core `reference` + `project` | FR-3…6, алгоритмы 2–3 |
| W3 | core `scoring` → calc | FR-7…9, UC-5, UC-7 |
| W4 | finance (parse по fileId + NPV) + core `finance` | FR-10…12, UC-10, алгоритм 1 |
| W5 | core `portfolio`: job, постпроверка, утверждение, STALE; `activity` (audit) | FR-13…17, 20, 23–24, UC-9 |
| W6 | UI-сценарии, отчёт в FTS, уведомления (in-app, email stub) | FR-18…19, UI-тесты, деплой |

Каждая wave — отдельная Spec Kit фича в соответствующем сервисе.

---

## 9. Открытые вопросы

| ID | Вопрос | Предложение |
|----|--------|-------------|
| DEC-MATH-02 | Решатель 0-1 | своя ДП/branch-and-bound в calc; ojAlgo — если понадобятся доп. ограничения |
| DEC-MATH-01 | TOPSIS | позже, отдельным endpoint в calc |
| Q-DOM-02/05 | Периоды и валюты | v1: один бюджет, одна валюта |
| BOOT-ADMIN | Seed Admin | перейти на env-bootstrap «только на пустой БД» |
| GW-STACK | Spring Cloud Gateway: WebFlux или Server WebMVC | **решено:** Server WebMVC — servlet-стек, переиспользует common-security без изменений; Spring Cloud 2025.1.x (≥ 2025.1.2) для Boot 4.1 |
| FTS-UPLOAD | Загрузка из браузера напрямую в FTS или через core | **решено:** через core (core проверяет права и сразу связывает fileId с сущностью) |
