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
- No token issuance, no DB, **no Redis** (session check goes to identity)
- GraphQL (`POST /graphql`, GraphiQL `/graphiql`) — чтение карточки/реестра, `projectRegistryStats` и бизнес-мутации жизненного цикла (register / submit / review / evaluate / approve / return / reject / archive), не CRUD. Resolver сразу отдаёт запрос в `PortfolioQueryService` / `PortfolioCommandService`. Подразделение и цели приходят через `DomainProjectCatalog`, инициатор через `ParticipantDirectory`. Сейчас оба порта — заглушки без сети (ЛР4). Реальный gRPC вместо заглушек — следующая лаба, не этот этап.

### GraphQL security (ЛР5)

`GraphqlSecurityInterceptor`: Bearer JWT (`AccessTokenVerifier`) → `IdentitySessionClient` → `GET {IDENTITY_URL}/api/v1/auth/sessions/active` → RBAC (`GraphqlOperationAccess`). Errors: `extensions.code` = `UNAUTHORIZED` / `FORBIDDEN`. Internal services must not re-check JWT.

Канон маршрутов: `01_ARCHITECTURE_Portfolio_Optimizer.md` §4. RBAC-матрица: корневой `AGENTS.md`.

## How to run

```bash
mvn -f libs/common-security/pom.xml install
mvn -f services/gateway/pom.xml package
CORE_JWT_SECRET='local-dev-secret-at-least-32-bytes!!' \
  IDENTITY_URL=http://localhost:8080 \
  java -jar services/gateway/target/gateway.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes) shared with identity. Identity (+ Redis) must be up for GraphQL business calls.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
