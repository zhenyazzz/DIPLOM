# Identity — guide for agents

Spring Boot app (`com.portfolio:identity-impl`), API module `com.portfolio:identity-api`.  
Package root `com.portfolio.identity`. Port **8080**. Issues JWT; other services only validate. Owns **Redis access sessions** (ЛР5).

## What this service is

- Users, roles (1:N: `roles` ← `users.role_id`), block/unblock, register/login/refresh/logout
- Schema `identity`: `users`, `roles`, `refresh_sessions` (PostgreSQL)
- JWT claim: `role` = `roles.code`, plus `jti` for the access session
- **Redis** active access sessions: key `identity:access-session:{jti}`, TTL = access token TTL; created on login/refresh, revoked on logout (all sessions for user)
- Session check for gateways: `GET /api/v1/auth/sessions/active` (Bearer JWT; 200 + payload if Redis still holds `jti`, else 401)
- Admin: `GET /api/v1/admin/users`, `PUT …/{id}/role`, `PUT …/{id}/blocked` (gateway must route `/api/v1/admin/**`)
- Refresh cookie path `/api/v1/auth` (gateway must not StripPrefix)

Out of scope: projects, finance, portfolio domain; M:N user_roles; permissions table; GraphQL.

## How to run

```bash
# infra: Postgres + Redis
docker compose -f infra/docker-compose.yml up -d postgres redis

mvn -f libs/common-security/pom.xml install
mvn -f libs/common-web/pom.xml install
mvn -f services/identity/pom.xml package
CORE_JWT_SECRET='local-dev-secret-at-least-32-bytes!!' \
  java -jar services/identity/identity-impl/target/identity.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes). Redis: `REDIS_HOST` / `REDIS_PORT` (default localhost:6379).

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
