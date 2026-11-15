# Контракты

OpenAPI-спецификации для согласования API между сервисами и frontend.

## Содержимое

| Файл | Сервис |
|------|--------|
| [`openapi/identity-v1.yaml`](openapi/identity-v1.yaml) | identity (auth, users) |
| [`openapi/domain-v1.yaml`](openapi/domain-v1.yaml) | domain (reference, projects, scoring, scenarios, runs, audit) |
| [`openapi/finance-v1.yaml`](openapi/finance-v1.yaml) | finance (imports, snapshots) |
| [`openapi/calc-v1.yaml`](openapi/calc-v1.yaml) | calc (SAW, 0-1) — internal |
| [`openapi/fts-v1.yaml`](openapi/fts-v1.yaml) | fts `/api/v1/files` — internal |
| [`openapi/notify-v1.yaml`](openapi/notify-v1.yaml) | notify (REST + internal events; WS отдельно) |

Канон границ: `docs/architecture/ARCHITECTURE.md`, полный: `01_ARCHITECTURE_Portfolio_Optimizer.md`.
