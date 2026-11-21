# Identity — guide for agents

Spring Boot app (`com.portfolio:identity-impl`), API module `com.portfolio:identity-api`.  
Package root `com.portfolio.identity`. Port **8080**. Issues JWT; other services only validate.

## What this service is

- Users, roles (1:N: `roles` ← `users.role_id`), block/unblock, register/login/refresh/logout
- Schema `identity`: `users`, `roles`, `refresh_sessions`
- JWT claim: `role` = `roles.code`
- Admin: `GET /api/v1/admin/users`, `PUT …/{id}/role`, `PUT …/{id}/blocked` (gateway must route `/api/v1/admin/**`)
- Refresh cookie path `/api/v1/auth` (gateway must not StripPrefix)

Out of scope: projects, finance, portfolio domain; M:N user_roles; permissions table.

## How to run

```bash
mvn -f libs/common-security/pom.xml install
mvn -f libs/common-web/pom.xml install
mvn -f services/identity/pom.xml package
java -jar services/identity/identity-impl/target/identity.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes).

## Flyway

Правки схемы вноси в уже существующий файл миграции этого сервиса. Новый `V{n}__….sql` для исправления или дополнения не создавай. Это правило репозитория; оно важнее совета в `02_DATABASE_Portfolio_Optimizer.md` §14.2 заводить новую версию.

## Lombok

Сущности — `@Getter`, `@Setter`, `@NoArgsConstructor`. Сервисы и контроллеры — `@RequiredArgsConstructor`. Неизменяемые DTO — `record`. `@Value` не используй.
