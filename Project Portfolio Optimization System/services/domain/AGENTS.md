# domain service

Port **8084**. Schema `domain`.

Единый предметный контур (пакеты, не отдельные процессы):

| Пакет | Ответственность |
|-------|-----------------|
| `reference/` | departments, budget_periods, strategic_goals |
| `project/` | projects, links, goals, attachments → FTS |
| `scoring/` | criteria, models, assessments, scores → calc SAW |
| `portfolio/` | scenarios, runs (async job), approvals (`GET /api/v1/approvals`, `POST /api/v1/scenarios/{id}/approvals`), reports → calc binary, finance, FTS |
| `audit/` | audit_log |
| `shared/` | клиенты, Money/ошибки |

Вызывает: calc, finance, fts, notify (afterCommit).  
Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md`.

Scaffold — таблицы и OpenAPI в волнах W2–W5.

Чтение карточки инициативы: `ProjectQueryService` и `GET /api/v1/projects`. Только схема `domain`. Инициатор хранится как UUID, в identity сервис не ходит.

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
