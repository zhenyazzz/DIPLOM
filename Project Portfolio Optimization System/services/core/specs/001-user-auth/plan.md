# Implementation Plan: User Accounts & Session Auth

**Branch**: `001-user-auth` | **Date**: 2026-10-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-user-auth/spec.md`

## Summary

Deliver the first vertical of `services/core`: user accounts with four roles, email/password auth, hybrid session (15вЂ‘minute JWT access in JSON + opaque refresh in HttpOnly cookie), multi-device refresh sessions, Admin user management, and empty-DB Admin bootstrap via env. This feature also bootstraps the Maven `api`/`impl` skeleton required by the constitution. No calc/finance or UI work.

## Technical Context

**Language/Version**: Java 25 LTS (pin toolchain in parent POM; must be within Spring Boot 4.1.x support)

**Primary Dependencies**: Spring Boot 4.1.x (latest patch at bootstrap), Spring Security, OAuth2 Resource Server / Nimbus JWT, Spring Data JPA, Flyway, MapStruct, Bean Validation, PostgreSQL driver

**Storage**: PostgreSQL; Flyway migrations in `impl`

**Testing**: JUnit 5, Spring Boot Test, MockMvc or WebTestClient, Testcontainers PostgreSQL

**Target Platform**: JVM server (local Docker Compose / standalone jar)

**Project Type**: Multi-module Maven web service (`api` + `impl`) under `services/core`

**Performance Goals**: Auth endpoints responsive for coursework demos (refresh &lt; 5s locally per SC-006); no high-load targets

**Constraints**: Hybrid session locked; no Keycloak; no ban-cache; no per-request blocked check on access JWT in v1; BigDecimal N/A; no calc/finance calls in this feature; YAGNI

**Scale/Scope**: Dozens of users for demos; tables `users`, `refresh_sessions`; REST under `/api/v1`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Domain-First Core | PASS | Users/roles owned by core; no SAW/knapsack here |
| II. Money & Weights Safety | PASS (N/A) | No money/weights in this feature |
| III. Explicit Integration | PASS (N/A) | No calc/finance calls yet |
| IV. Test Pyramid | PASS | Plan requires unit + integration tests; UI out of core |
| V. Simplicity | PASS | No Kafka/K8s/Keycloak; Postgres + Flyway only |
| VI. Modular Maven API/Impl | PASS | Skeleton + records/MapStruct in scope |

**Post-design re-check**: PASS вЂ” contracts in `api`, behavior in `impl`, OpenAPI documents public surface; research avoids speculative infra.

## Project Structure

### Documentation (this feature)

```text
specs/001-user-auth/
в”њв”Ђв”Ђ plan.md
в”њв”Ђв”Ђ research.md
в”њв”Ђв”Ђ data-model.md
в”њв”Ђв”Ђ quickstart.md
в”њв”Ђв”Ђ contracts/
в”‚   в””в”Ђв”Ђ openapi.yaml
в””в”Ђв”Ђ tasks.md             # (/speckit-tasks вЂ” not this command)
```

### Source Code (repository root = `services/core`)

```text
services/core/
в”њв”Ђв”Ђ pom.xml                          # parent: portfolio-core
в”њв”Ђв”Ђ portfolio-core-api/
в”‚   в”њв”Ђв”Ђ pom.xml
в”‚   в””в”Ђв”Ђ src/main/java/com/portfolio/core/api/
в”‚       в”њв”Ђв”Ђ auth/                    # AuthApi interface, *Request/*Response records
в”‚       в”њв”Ђв”Ђ user/                    # UsersApi (me), UserResponse, UserRole
в”‚       в””в”Ђв”Ђ admin/                   # AdminUsersApi
в””в”Ђв”Ђ portfolio-core-impl/
    в”њв”Ђв”Ђ pom.xml
    в””в”Ђв”Ђ src/
        в”њв”Ђв”Ђ main/
        в”‚   в”њв”Ђв”Ђ java/com/portfolio/core/
        в”‚   в”‚   в”њв”Ђв”Ђ CoreApplication.java
        в”‚   в”‚   в”њв”Ђв”Ђ auth/            # services, cookie helper, token issuer
        в”‚   в”‚   в”њв”Ђв”Ђ user/            # domain, repos, AdminUserService
        в”‚   в”‚   в”њв”Ђв”Ђ security/        # SecurityFilterChain, JWT converter
        в”‚   в”‚   в”њв”Ђв”Ђ bootstrap/       # AdminBootstrapRunner
        в”‚   в”‚   в”њв”Ђв”Ђ config/          # props, MapStruct, OpenAPI optional
        в”‚   в”‚   в””в”Ђв”Ђ web/             # @RestController implements api interfaces
        в”‚   в””в”Ђв”Ђ resources/
        в”‚       в”њв”Ђв”Ђ application.yml
        в”‚       в””в”Ђв”Ђ db/migration/V1__users_and_refresh_sessions.sql
        в””в”Ђв”Ђ test/java/com/portfolio/core/           # unit + @SpringBootTest + Testcontainers
```

**Structure Decision**: Constitution-mandated parent + `api` + `impl`. Base package `com.portfolio.core` (not university-prefixed). Controllers in `impl` implement interfaces from `api`. Frontend remains outside this tree (`../../frontend`).

## Complexity Tracking

> No constitution violations requiring justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| вЂ” | вЂ” | вЂ” |

## Phase 0 вЂ” Research

Completed: [research.md](./research.md)

Key locks: HS256 JWT access 15m; opaque refresh SHA-256 in DB, 14d; cookie `refresh_token` HttpOnly Path=`/api/v1/auth` SameSite=Lax; BCrypt; env bootstrap Admin; Nimbus; Testcontainers; no CSRF token in v1.

## Phase 1 вЂ” Design

| Artifact | Path |
|----------|------|
| Data model | [data-model.md](./data-model.md) |
| API contract | [contracts/openapi.yaml](./contracts/openapi.yaml) |
| Validation guide | [quickstart.md](./quickstart.md) |

### Design notes (for tasks)

1. **Security filter chain**: permit `POST` register/login/refresh/logout; authenticate all other `/api/v1/**` with Bearer JWT; `/api/v1/admin/**` requires `ROLE_ADMIN`.
2. **Cookie on refresh/logout**: controllers must read/write `HttpServletResponse` Set-Cookie; `api` interfaces may accept abstracted cookie handling in impl adapters.
3. **Error shape**: shared `ErrorResponse(code, message, details?)` вЂ” uniform `UNAUTHORIZED` for bad login.
4. **Idempotent logout**: missing/invalid cookie в†’ still 204 + clear cookie header.
5. **Email normalize**: store/compare lower-case.
6. **Pin versions** in parent POM at first `mvn` bootstrap (Boot 4.1.x patch, MapStruct, etc.).

## Next command

`/speckit-tasks` вЂ” break plan into dependency-ordered implementation tasks.
