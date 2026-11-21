# notify service

Port **8087**. Schema `notify`.

- Public: `/api/v1/notifications/**` (через gateway)
- WebSocket: `/ws/**` (nginx → notify напрямую, не через gateway)
- Internal: `POST /internal/v1/events` (domain/finance afterCommit)

Таблицы: `notifications`, `delivery_attempts`.  
Email: stub (лог) до W6; доставка не откатывает бизнес-транзакцию.

Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md` §9.

События от domain и finance notify дальше получает из Kafka. В ЛР3 брокер не подключается: лаба оставляет межсервисный обмен на заглушках.

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
