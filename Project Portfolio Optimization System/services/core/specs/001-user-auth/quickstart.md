# Quickstart Validation: 001-user-auth

Manual / scripted checks after implementation. Details: [data-model.md](./data-model.md), [contracts/openapi.yaml](./contracts/openapi.yaml).

## Prerequisites

- JDK per constitution (Java 25 LTS target)
- Maven 3.9+
- PostgreSQL (Docker Compose from repo `infra/` when wired, or local)
- Env for first boot:
  - `CORE_JWT_SECRET` (≥32 random chars)
  - `CORE_COOKIE_SECURE=false` for local HTTP
- Seed admin (Flyway `V2__seed_admin_user.sql`): `admin@portfolio.local` / `Admin123!`

## Setup (expected after implement)

```bash
# from services/core
mvn -q verify
# run impl module (exact module name from parent POM)
mvn -pl portfolio-core-impl spring-boot:run
```

Base URL: `http://localhost:8080`

## Scenario A — Seed Admin + me

1. Start on empty DB → Flyway seeds Admin (`admin@portfolio.local` / `Admin123!`).
2. `POST /api/v1/auth/login` with seed email/password.
3. Expect `200` + `accessToken` + `Set-Cookie: refresh_token=...; HttpOnly; Path=/api/v1/auth; SameSite=Lax`.
4. `GET /api/v1/users/me` with `Authorization: Bearer <accessToken>` → role `ADMIN`.
5. Restart app → seed `ON CONFLICT` must not create a second Admin.

## Scenario B — Register Initiator + forbidden admin

1. `POST /api/v1/auth/register` new email → `201`, role `INITIATOR`.
2. Login as that user → access token.
3. `GET /api/v1/admin/users` → `403`.

## Scenario C — Refresh rotation + logout

1. Login; save cookie jar.
2. `POST /api/v1/auth/refresh` with cookie only → new access + new Set-Cookie.
3. Replay old refresh cookie → `401`.
4. `POST /api/v1/auth/logout` with current cookie → `204`.
5. Refresh again → `401`.

## Scenario D — Multi-session

1. Login twice (two cookie jars) for same user.
2. Logout jar A → jar B refresh still works.
3. Admin blocks user → both jars fail refresh; login fails.
4. Note: old access JWT from before block may work until 15 min TTL (accepted).

## Scenario E — Last Admin protection

1. As sole Admin, attempt block self or demote self → `409`.
2. Promote another user to Admin, then demote/block original → allowed.

## Scenario F — Uniform login failure

1. Wrong password for existing email → `401` same body shape as unknown email.
2. No field revealing “user exists”.

## Automated gate

```bash
mvn -pl portfolio-core-impl test
```

Must cover: login, refresh, logout, register role, admin 403, block+refresh revoke (per FR-015).

## Frontend note (out of scope code)

Later React client: store access in memory; `credentials: 'include'` on `/api/v1/auth/*`; never read refresh cookie from JS.
