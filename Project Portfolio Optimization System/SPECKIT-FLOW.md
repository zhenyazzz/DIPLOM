# Spec Kit: как писать спецификацию в `services/core`

Где запускать команды:

| Проект | Путь | Пакет |
|--------|------|--------|
| **core** | `services/core` | `com.portfolio.core` — модули `portfolio-core` / `api` / `impl` |

Skills Spec Kit: `.cursor/skills/` **внутри того проекта**, из которого открыт чат.

`libs/common-web` / `libs/common-security` — библиотеки; для агентов см. `libs/common-web/AGENTS.md` (не Spec Kit).

Команды вида `/speckit-*` — это Spec Kit skills, не bash.

---

## Весь флоу (по порядку)

| # | Команда | Зачем | Когда |
|---|--------|--------|--------|
| 1 | `/speckit-constitution` | правила проекта (стек, деньги, тесты, границы сервисов) | **сейчас, первым** |
| 2 | `/speckit-specify` | фича: user stories, acceptance criteria | после constitution |
| 3 | `/speckit-clarify` | до 5 уточнений, дописывает в spec | если spec дырявый |
| 4 | `/speckit-plan` | техплан, модели, API, интеграции | после approve spec |
| 5 | `/speckit-tasks` | чеклист задач под implement | после approve plan |
| 6 | `/speckit-analyze` | сверка spec/plan/tasks | перед кодом (желательно) |
| 7 | `/speckit-implement` | пишет код по tasks | когда tasks ок |

Опционально позже: `/speckit-checklist`, `/speckit-converge`, `/speckit-taskstoissues`.

---

## Шаг 1 — первая команда: constitution

Вставь в чат в `services/core`:

```text
/speckit-constitution

Проект: core-сервис системы формирования и оптимизации портфеля проектов предприятия (BSUIR ИИСиК КР).

Принципы:
1. Domain-first core — core владеет реестром проектов, критериями, сценариями, пользователями/ролями, бюджетом портфеля и оркестрацией. Математику (SAW, 0-1 knapsack) НЕ считает сам — вызывает calc-service по HTTP. Парсинг финансовых файлов — finance-service (позже).
2. Money & weights safety — все денежные суммы, веса критериев и результаты скоринга только BigDecimal; double/float для денег запрещены.
3. Explicit integration — любой вызов calc/finance: версионированный REST-контракт, таймауты, понятные ошибки; core не падает молча при недоступности calc.
4. Test pyramid — unit на доменную логику, integration на API/БД и контракт с calc, UI-тесты на фронте отдельно; без зелёных тестов фича не считается готовой.
5. Simplicity for coursework — без Kafka/K8s/ESB; PostgreSQL + Flyway; Spring Boot modular monolith внутри core; YAGNI.

Стек (зафиксировать в constitution):
- Java 21, Spring Boot 3.x, Spring Security, PostgreSQL, Flyway
- REST JSON API для frontend
- calc-service: stateless HTTP (SAW + knapsack)
- finance-service: позже, заглушка допустима

Роли минимум: Initiator, Expert/Analyst, Decision Maker, Admin.

Governance: constitution важнее локальных привычек; изменения — через /speckit-constitution с bump версии.
Ratified: 2026-10-18
```

Результат: заполненный `.specify/memory/constitution.md`. Проверить и сказать «ок» / что поправить.

---

## Шаг 2 — первая фича: specify

Не описывать «всю систему». Одна вертикаль:

```text
/speckit-specify

Feature: Project Registry & Portfolio Budget Baseline для core.

Как Initiator я создаю/редактирую кандидатные проекты (название, описание, запрашиваемый бюджет, статус черновик/на рассмотрении).
Как Analyst я задаю бюджет портфеля на период и вижу список кандидатов с фильтрами.
Как Admin я управляю пользователями и ролями (минимум 3 роли).

Out of scope сейчас: SAW/knapsack (это calc), импорт CSV/XLSX (finance), полноценный UI (только API core).
Нужны: сущности под 3NF, REST API, Flyway-миграции, аудит базовых изменений.
Acceptance: CRUD проектов с валидацией бюджета > 0; нельзя утвердить портфель без бюджета; роли разграничены на уровне API.
```

Результат: что-то вроде `specs/001-project-registry/spec.md`. Прочитать и поправить формулировки.

---

## Шаги 3–7 (коротко)

1. `/speckit-clarify` — если в spec дыры (статусы, валюта, кто может менять бюджет).
2. `/speckit-plan` — Spring-модули, таблицы, эндпоинты, как дергать calc (даже если calc stub).
3. Проверить plan → `/speckit-tasks`.
4. `/speckit-analyze` → поправить рассинхрон.
5. `/speckit-implement` — только когда tasks выглядят нормально.

Параллельно в `services/calc` тот же цикл, но фича про `POST score/optimize`, без БД.

---

## Важно

- **Constitution** = правила навсегда для агента.
- **Specify** = одна фича за раз.
- Не запускать `/speckit-implement` до нормального spec + plan + tasks.
- Git пока не обязателен; если hook попросит branch — можно сказать «без git, только specs/».

---

## Сейчас

1. Открыть чат в `services/core`.
2. Вставить блок `/speckit-constitution` выше.
3. Дождаться файла, проверить.
4. Перейти к `/speckit-specify`.
