# Project Portfolio Optimizer

## Текст

Тире в текстах и отчётах – символ «–», не дефис и не длинное тире.

## GraphQL (gateway, ЛР4)

Внешний GraphQL API: Query (`project`, `projects` с фильтром/сортировкой, `projectRegistryStats`) и бизнес-Mutation жизненного цикла инициативы. Не CRUD и не прямой доступ к БД. Межсервисные порты пока заглушки; реальный gRPC – следующая лабораторная. Детали: `services/gateway/AGENTS.md`, запись в PostgreSQL: `services/domain/AGENTS.md`.

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
