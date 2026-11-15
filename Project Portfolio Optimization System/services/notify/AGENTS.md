# notify service

Port **8087**. Schema `notify`.

- Public: `/api/v1/notifications/**` (через gateway)
- WebSocket: `/ws/**` (nginx → notify напрямую, не через gateway)
- Internal: `POST /internal/v1/events` (domain/finance afterCommit)

Таблицы: `notifications`, `delivery_attempts`.  
Email: stub (лог) до W6; доставка не откатывает бизнес-транзакцию.

Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md` §9.
