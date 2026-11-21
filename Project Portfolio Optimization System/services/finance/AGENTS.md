# finance service

Port **8082**. Schema `finance`.

Import sessions, snapshots, cash flows, parse and NPV; owns financial facts.  
Calls FTS for file bytes; afterCommit → notify on publish/fail.

Scaffold — business logic in W4. Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md`.

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
