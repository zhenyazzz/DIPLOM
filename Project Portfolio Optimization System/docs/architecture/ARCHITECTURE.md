# Architecture — Project Portfolio Optimization System

**Статус:** `accepted` (2026-11-15).  
**Полный канон:** [`01_ARCHITECTURE_Portfolio_Optimizer.md`](../../01_ARCHITECTURE_Portfolio_Optimizer.md)  
**БД:** [`02_DATABASE_Portfolio_Optimizer.md`](../../02_DATABASE_Portfolio_Optimizer.md)

## Решения

| ID | Решение |
|----|---------|
| ARCH-01 | 7 процессов + SPA: gateway, identity, **domain**, finance, calc, fts, **notify**; без Kafka/K8s |
| ARCH-02 | project+scoring+portfolio = **один** `domain` (пакеты); Modulith-core отменён |
| ARCH-03 | Finance владеет импортами/снимками + parse/NPV |
| ARCH-04 | Job оптимизации в domain; calc sync/stateless |
| ARCH-05 | Boot + libs + OpenAPI; клиенты у потребителя |
| ARCH-06 | FTS — единственный MinIO |
| ARCH-07 | Spring Cloud Gateway (WebMVC) |
| ARCH-08 | Одна PG, schema-per-service; UUID без кросс-FK |
| ARCH-09 | notify — WebSocket + email; доставка ≠ откат бизнеса |
| ARCH-10 | scoring-данные в domain; calc — только формулы |

## Топология

```text
browser → nginx → gateway
            ├─ auth/users     → identity
            ├─ projects, criteria, scenarios, runs, audit → domain → calc, finance, fts, notify
            ├─ finance/**     → finance → fts
            └─ notifications  → notify
browser → nginx /ws/** → notify
calc (stateless) · fts (MinIO) · PG schemas: identity, domain, finance, notify, fts
```

**Не дробить domain дальше** — один BC «реестр → оценка → решение».  
**STALE** — только domain (веса сразу; snapshot — lazy при чтении/start run).  
**calc/fts/internal** — не на gateway, только docker-сеть.  
Детали API, волн и notify: полный документ `01_ARCHITECTURE_…`.
