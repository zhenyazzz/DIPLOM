# Identity — guide for agents

Spring Boot app (`com.portfolio:identity-impl`), API module `com.portfolio:identity-api`.  
Package root `com.portfolio.identity`. Port **8080**. Issues JWT; other services only validate.

## What this service is

- Users, roles (1:N: `roles` ← `users.role_id`), block/unblock, register/login/refresh/logout
- Schema `identity`: `users`, `roles`, `refresh_sessions`
- JWT claim: `role` = `roles.code`
- Admin: `GET /api/v1/admin/users` (paged + RSQL `filter`), `PUT …/{id}/role`, `PUT …/{id}/blocked` (gateway must route `/api/v1/admin/**`)
- Refresh cookie path `/api/v1/auth` (gateway must not StripPrefix)

Out of scope: projects, finance, portfolio domain; M:N user_roles; permissions table.

## Libraries used

| Library | What identity uses | What identity does **not** use |
|---------|--------------------|--------------------------------|
| `common-security` | Auto JWT validation, `portfolio.security.*` config, `SecurityFilterChain`, `@PreAuthorize`, `SecuritySupport.requireUserId()`, Problem Details 401/403 from the filter chain | Token issuance (stays in `JwtTokenService`) |
| `common-web` | `@EnableCommonWeb(problemDetails = true)`; admin list composes `PageResponse`, `FieldAllowList` / `FieldRule`, `RsqlSpecificationBuilder`, `CommonWebProperties` (page-size clamp); kit `CommonWebProblemDetailsHandler` for `BAD_FILTER` | Full **CRUD kit** wiring: `BaseEntity`, `BaseRepository`, `AbstractJpaCrudService`, `AbstractCrudController`, `EntityMapper` (CUD stays custom) |

### Admin user list (paged + filter)

`GET /api/v1/admin/users` matches the kit list contract (`Pageable` + optional RSQL `filter` → `PageResponse`):

- Allow-listed root fields only: `email` (String), `displayName` (String), `blocked` (boolean) — filter + sort
- Nested paths (e.g. `role.code`) are rejected by common-web RSQL (no join filters)
- Never allow-list `passwordHash` or audit secrets
- `UserRepository` extends `JpaRepository` + `JpaSpecificationExecutor` (not `BaseRepository` — that requires `BaseEntity`)

### Why full CRUD kit is not used for users / admin

Admin and profile APIs are **custom domain endpoints**, not ordinary resource CRUD:

- `changeRole` / `setBlocked` with last-active-admin guard
- Session revoke on block
- Auth register/login/refresh/logout + refresh cookies
- Profile `GET /users/me` (current JWT subject only)

`common-web` AGENTS.md: keep custom endpoints (auth, orchestration, privileged ops) as separate controllers/services. Do **not** force `UserEntity` onto `BaseEntity` / Abstract CRUD just to “use the lib” — reuse search/page helpers by composition instead.

## Package layout

| Module | Package | Contents |
|--------|---------|----------|
| `identity-api` | `com.portfolio.identity.api.auth` | Auth API contract (`AuthApi`) |
| `identity-api` | `com.portfolio.identity.api.auth.dto` | Auth request/response DTOs |
| `identity-api` | `com.portfolio.identity.api.identity` | Users / admin API contracts + `UserRole` |
| `identity-api` | `com.portfolio.identity.api.identity.dto` | Users / admin request/response DTOs |
| `identity-impl` | `com.portfolio.identity.config` | `AuthProperties`, beans |
| `identity-impl` | `com.portfolio.identity.error` | `IdentityException` + RFC 9457 Problem Details handler |
| `identity-impl` | `com.portfolio.identity.identity.entity` | `UserEntity`, `RoleEntity` |
| `identity-impl` | `com.portfolio.identity.identity.repository` | User / role repositories |
| `identity-impl` | `com.portfolio.identity.identity.mapper` | `UserMapper` |
| `identity-impl` | `com.portfolio.identity.identity.service` | Profile + admin services |
| `identity-impl` | `com.portfolio.identity.identity.web` | Users / admin controllers |
| `identity-impl` | `com.portfolio.identity.auth.entity` | `RefreshSessionEntity` |
| `identity-impl` | `com.portfolio.identity.auth.repository` | Refresh session repository |
| `identity-impl` | `com.portfolio.identity.auth.service` | Auth, JWT issue, refresh cookie/session |
| `identity-impl` | `com.portfolio.identity.auth.web` | Auth controller |

## Security

```yaml
portfolio:
  security:
    jwt:
      secret: ${CORE_JWT_SECRET}   # same HS256 secret as issuance (app.jwt.secret)
    permit-all:
      - POST:/api/v1/auth/register
      - POST:/api/v1/auth/login
      - POST:/api/v1/auth/refresh
      - POST:/api/v1/auth/logout
```

- Privileged admin ops: `@PreAuthorize("hasRole('ADMIN')")` on `AdminUsersController`
- Current user id: `SecuritySupport.requireUserId()` in `UserQueryService` (not hand-rolled JWT parsing)

## Errors

RFC 9457 `ProblemDetail` + property `code` (same shape as `common-security` / FTS):

| Status | `code` | Source |
|--------|--------|--------|
| 401 | `UNAUTHORIZED` | `IdentityException`, `UnauthenticatedException`, or `common-security` filter |
| 403 | `FORBIDDEN` | `common-security` method security |
| 404 | `NOT_FOUND` | `IdentityException` |
| 400 | `BAD_REQUEST` | `IdentityException` / validation |
| 400 | `BAD_FILTER` | common-web `FilterBadRequestException` (bad RSQL / sort / allow-list) |
| 409 | `CONFLICT` | `IdentityException` (e.g. last admin) |
| 500 | `INTERNAL_ERROR` | unhandled |

`IdentityExceptionHandler` owns domain + validation; kit handler from `@EnableCommonWeb` stays available for kit exceptions (`BAD_FILTER`, etc.). No `ErrorResponse` DTO.

## How to run

```bash
mvn -f libs/common-security/pom.xml install
mvn -f libs/common-web/pom.xml install
mvn -f services/identity/pom.xml package
java -jar services/identity/identity-impl/target/identity.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes).
