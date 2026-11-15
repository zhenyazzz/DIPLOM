# Gateway — guide for agents

Spring Boot app (`com.portfolio:portfolio-gateway`), single module. Port **8090**.  
Spring Cloud Gateway Server WebMVC (servlet stack).

## What this service is

Single entry point behind nginx for `/api/**`. Routes by prefix (no StripPrefix):

| Path | Upstream env | Default |
|------|--------------|---------|
| `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/admin/**` | `IDENTITY_URL` | `http://localhost:8080` |
| `/api/v1/reference/**`, `/api/v1/projects/**`, `/api/v1/criteria/**`, `/api/v1/scoring-models/**`, `/api/v1/assessments/**`, `/api/v1/scores/**`, `/api/v1/scenarios/**`, `/api/v1/runs/**`, `/api/v1/approvals/**`, `/api/v1/reports/**`, `/api/v1/audit/**` | `DOMAIN_URL` | `http://localhost:8084` |
| `/api/v1/finance/**` | `FINANCE_URL` | `http://localhost:8082` |
| `/api/v1/notifications/**` | `NOTIFY_URL` | `http://localhost:8087` |

- Early JWT via `common-security`; login rate-limit; CORS
- `/api/v1/files/**` → 404 (FTS internal)
- `/ws/**` не здесь — nginx → notify напрямую
- No business logic, no token issuance, no DB

Канон маршрутов: `01_ARCHITECTURE_Portfolio_Optimizer.md` §4.

## How to run

```bash
mvn -f libs/common-security/pom.xml install
mvn -f services/gateway/pom.xml package
java -jar services/gateway/target/gateway.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes).
