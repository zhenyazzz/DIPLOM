# Project Portfolio Optimizer

## Текст

Тире в текстах и отчётах – символ «–», не дефис и не длинное тире.

## GraphQL (gateway, ЛР4)

Внешний GraphQL API: Query (`project`, `projects` с фильтром/сортировкой, `projectRegistryStats`) и бизнес-Mutation жизненного цикла инициативы. Не CRUD и не прямой доступ к БД. Межсервисные порты пока заглушки; реальный gRPC – следующая лабораторная. Детали: `services/gateway/AGENTS.md`, запись в PostgreSQL: `services/domain/AGENTS.md`.

## Security (ЛР5)

- **Auth Service** = `services/identity` (порт 8080): регистрация, логин/refresh/logout, выпуск JWT, роли, **активные access-сессии в Redis** (TTL = access TTL). Refresh по-прежнему в PostgreSQL.
- **Gateway GraphQL** не хранит пользователей и не проверяет пароль. Перед Query/Mutation: JWT → проверка активной сессии через identity (`GET /api/v1/auth/sessions/active`) → RBAC по роли из JWT.
- Валидный JWT **без** ключа в Redis = `UNAUTHORIZED`. Недостаточно прав на операцию = `FORBIDDEN` (`extensions.code`).

### RBAC (GraphQL root fields)

| Операции | Роли |
|----------|------|
| `project`, `projects`, `projectRegistryStats` | INITIATOR, EXPERT_ANALYST, DECISION_MAKER, ADMIN |
| `registerInitiative`, `submitInitiative` | INITIATOR, ADMIN |
| `startReview`, `completeEvaluation` | EXPERT_ANALYST, ADMIN |
| `approveForPortfolio`, `returnForRevision`, `rejectInitiative` | DECISION_MAKER, ADMIN |
| `archiveInitiative` | ADMIN |

Локально: Redis + identity + gateway (см. `infra/docker-compose.yml`, `REDIS_HOST` / `IDENTITY_URL`).

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
