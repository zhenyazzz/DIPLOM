# domain service

Port **8084**. Schema `domain`.

Единый предметный контур (пакеты, не отдельные процессы):

| Пакет | Ответственность |
|-------|-----------------|
| `reference/` | departments, budget_periods, strategic_goals |
| `project/` | projects, links, goals, attachments → FTS |
| `scoring/` | criteria, models, assessments, scores → calc SAW |
| `portfolio/` | scenarios, runs (async job), approvals, reports → calc binary, finance, FTS |
| `audit/` | audit_log |
| `shared/` | клиенты, Money/ошибки |

Вызывает: calc, finance, fts, notify (afterCommit).  
Канон: `01_ARCHITECTURE_Portfolio_Optimizer.md`.

Scaffold — таблицы и OpenAPI в волнах W2–W5.
