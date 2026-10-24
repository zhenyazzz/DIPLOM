---
description: "Task list for User Accounts & Session Auth implementation"
---

# Tasks: User Accounts & Session Auth

**Input**: Design documents from `/specs/001-user-auth/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included вЂ” FR-015 and constitution test pyramid require unit + integration coverage for login/refresh/logout/roles/block.

**Organization**: Phases by user story (US1вЂ“US5). Base package: `com.portfolio.core` (never `by.bsuir.*`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: US1вЂ¦US5 maps to spec user stories
- Paths relative to `services/core/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maven multi-module skeleton and pinned dependencies

- [x] T001 Create parent POM `pom.xml` (`portfolio-core`) with modules `portfolio-core-api`, `portfolio-core-impl`, Java 25 toolchain, Spring Boot 4.1.x latest patch BOM
- [x] T002 Create `portfolio-core-api/pom.xml` (jar; validation-api / jakarta validation; no Spring Boot app)
- [x] T003 Create `portfolio-core-impl/pom.xml` depending on api + Web, Security, OAuth2 Resource Server, Data JPA, Flyway, PostgreSQL, MapStruct, Testcontainers test deps
- [x] T004 [P] Add `portfolio-core-impl/src/main/java/com/portfolio/core/CoreApplication.java` and empty `application.yml` under `portfolio-core-impl/src/main/resources/`
- [x] T005 [P] Wire PostgreSQL + Flyway placeholders in `portfolio-core-impl/src/main/resources/application.yml` (datasource, jpa, flyway, jwt/cookie/bootstrap props)
- [x] T006 [P] Add Postgres service stub notes / env sample in `portfolio-core-impl/src/main/resources/application-local.yml.example` (email/password/jwt secret keys only вЂ” no secrets committed)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared domain, API contracts, security skeleton вЂ” MUST finish before story work

**вљ пёЏ CRITICAL**: No user story implementation until this phase completes

- [x] T007 [P] Add `UserRole` enum in `portfolio-core-api/src/main/java/com/portfolio/core/api/user/UserRole.java`
- [x] T008 [P] Add shared `ErrorResponse` (+ field detail record) in `portfolio-core-api/src/main/java/com/portfolio/core/api/common/ErrorResponse.java`
- [x] T009 [P] Add `UserResponse` record in `portfolio-core-api/src/main/java/com/portfolio/core/api/user/UserResponse.java`
- [x] T010 Create Flyway `portfolio-core-impl/src/main/resources/db/migration/V1__users_and_refresh_sessions.sql` per data-model.md
- [x] T011 [P] Create JPA `UserEntity` in `portfolio-core-impl/src/main/java/com/portfolio/core/user/UserEntity.java`
- [x] T012 [P] Create JPA `RefreshSessionEntity` in `portfolio-core-impl/src/main/java/com/portfolio/core/auth/RefreshSessionEntity.java`
- [x] T013 [P] Create `UserRepository` in `portfolio-core-impl/src/main/java/com/portfolio/core/user/UserRepository.java`
- [x] T014 [P] Create `RefreshSessionRepository` in `portfolio-core-impl/src/main/java/com/portfolio/core/auth/RefreshSessionRepository.java`
- [x] T015 Create `AuthProperties` / `JwtProperties` / cookie props in `portfolio-core-impl/src/main/java/com/portfolio/core/config/AuthProperties.java`
- [x] T016 Implement `PasswordEncoder` bean (BCrypt) in `portfolio-core-impl/src/main/java/com/portfolio/core/config/SecurityBeansConfig.java`
- [x] T017 Implement JWT issue/validate helper (Nimbus) in `portfolio-core-impl/src/main/java/com/portfolio/core/auth/JwtTokenService.java`
- [x] T018 Implement refresh cookie read/write/clear helper in `portfolio-core-impl/src/main/java/com/portfolio/core/auth/RefreshCookieService.java`
- [x] T019 Implement refresh session create/rotate/revoke/find-by-hash in `portfolio-core-impl/src/main/java/com/portfolio/core/auth/RefreshSessionService.java`
- [x] T020 Configure `SecurityFilterChain` + JWT resource-server converter in `portfolio-core-impl/src/main/java/com/portfolio/core/security/SecurityConfig.java` (permit auth public routes; protect `/api/v1/**`; `/api/v1/admin/**` в†’ `ROLE_ADMIN`)
- [x] T021 Implement global exception в†’ `ErrorResponse` in `portfolio-core-impl/src/main/java/com/portfolio/core/web/ApiExceptionHandler.java`
- [x] T022 [P] Add MapStruct `UserMapper` in `portfolio-core-impl/src/main/java/com/portfolio/core/user/UserMapper.java` (`UserEntity` в†’ `UserResponse`)
- [x] T023 Add Testcontainers base for integration tests in `portfolio-core-impl/src/test/java/com/portfolio/core/support/PostgresIntegrationTest.java`

**Checkpoint**: App starts against Postgres; security chain loads; migrations apply; no story endpoints required yet

---

## Phase 3: User Story 1 вЂ” Sign in / refresh / logout (Priority: P1) рџЋЇ MVP

**Goal**: Login returns access JWT + HttpOnly refresh cookie; refresh rotates; logout revokes current session only

**Independent Test**: Seed a user in DB/tests в†’ login в†’ me-capable token в†’ refresh в†’ logout в†’ refresh fails (quickstart C partially)

### Tests for User Story 1

- [x] T024 [P] [US1] Unit test JWT issue/validate in `portfolio-core-impl/src/test/java/com/portfolio/core/auth/JwtTokenServiceTest.java`
- [x] T025 [P] [US1] Unit test refresh hash/rotate rules in `portfolio-core-impl/src/test/java/com/portfolio/core/auth/RefreshSessionServiceTest.java`
- [x] T026 [US1] Integration test login/refresh/logout + Set-Cookie HttpOnly in `portfolio-core-impl/src/test/java/com/portfolio/core/auth/AuthSessionIntegrationTest.java` (fail first)

### Implementation for User Story 1

- [x] T027 [P] [US1] Add api records `LoginRequest`, `TokenResponse` in `portfolio-core-api/src/main/java/com/portfolio/core/api/auth/`
- [x] T028 [P] [US1] Add `AuthApi` interface (login/refresh/logout) in `portfolio-core-api/src/main/java/com/portfolio/core/api/auth/AuthApi.java`
- [x] T029 [US1] Implement `AuthService` login/refresh/logout (uniform 401 on bad login; multi-session create; rotate; revoke current) in `portfolio-core-impl/src/main/java/com/portfolio/core/auth/AuthService.java`
- [x] T030 [US1] Implement `AuthController` implementing `AuthApi` + cookie handling in `portfolio-core-impl/src/main/java/com/portfolio/core/web/AuthController.java`
- [x] T031 [US1] Ensure SecurityConfig permits `POST /api/v1/auth/login|refresh|logout` in `portfolio-core-impl/src/main/java/com/portfolio/core/security/SecurityConfig.java`
- [x] T032 [US1] Make T026 integration tests green (assert cookie attributes Path/HttpOnly/SameSite)

**Checkpoint**: MVP auth session works with a pre-seeded user

---

## Phase 4: User Story 2 вЂ” Self-register as Initiator (Priority: P2)

**Goal**: Public registration creates Initiator only; conflict on duplicate email

**Independent Test**: Register в†’ login в†’ admin endpoint 403

### Tests for User Story 2

- [x] T033 [US2] Integration test register + duplicate email + role INITIATOR in `portfolio-core-impl/src/test/java/com/portfolio/core/auth/RegisterIntegrationTest.java` (fail first)

### Implementation for User Story 2

- [x] T034 [P] [US2] Add `RegisterRequest` record with Bean Validation in `portfolio-core-api/src/main/java/com/portfolio/core/api/auth/RegisterRequest.java`
- [x] T035 [US2] Extend `AuthApi` + `AuthService`/`AuthController` with register в†’ 201 `UserResponse` in api/impl auth files
- [x] T036 [US2] Permit `POST /api/v1/auth/register` in `SecurityConfig.java`
- [x] T037 [US2] Make T033 green; assert registered user cannot call admin list (403) once admin route exists stub or after US5 вЂ” if admin route not yet present, assert role field only and defer 403 assert to US5 test

**Checkpoint**: Anyone can create Initiator accounts without Admin

---

## Phase 5: User Story 3 вЂ” Bootstrap first Admin (Priority: P2)

**Goal**: Env-based Admin seed only when `users` empty; second boot no-op

**Independent Test**: Empty DB + env в†’ Admin login; restart в†’ still one Admin

### Tests for User Story 3

- [x] T038 [US3] Integration test bootstrap creates Admin once in `portfolio-core-impl/src/test/java/com/portfolio/core/bootstrap/AdminBootstrapIntegrationTest.java` (fail first)

### Implementation for User Story 3

- [x] T039 [US3] Implement `AdminBootstrapRunner` reading `CORE_BOOTSTRAP_ADMIN_EMAIL` / `PASSWORD` in `portfolio-core-impl/src/main/java/com/portfolio/core/bootstrap/AdminBootstrapRunner.java`
- [x] T040 [US3] Document required env vars in `portfolio-core-impl/src/main/resources/application.yml` comments / example file
- [x] T041 [US3] Make T038 green (empty DB creates; non-empty skips)

**Checkpoint**: Deterministic Admin for demos without public bootstrap API

---

## Phase 6: User Story 4 вЂ” View my profile (Priority: P3)

**Goal**: `GET /api/v1/users/me` returns current user

**Independent Test**: Login в†’ me 200; no token в†’ 401

### Tests for User Story 4

- [x] T042 [US4] Integration test me success/401 in `portfolio-core-impl/src/test/java/com/portfolio/core/user/MeIntegrationTest.java` (fail first)

### Implementation for User Story 4

- [x] T043 [P] [US4] Add `UsersApi` interface in `portfolio-core-api/src/main/java/com/portfolio/core/api/user/UsersApi.java`
- [x] T044 [US4] Implement `UserQueryService` + `UsersController` in `portfolio-core-impl/src/main/java/com/portfolio/core/user/UserQueryService.java` and `.../web/UsersController.java`
- [x] T045 [US4] Make T042 green

**Checkpoint**: Clients can resolve identity/role from access token

---

## Phase 7: User Story 5 вЂ” Admin manages users (Priority: P3)

**Goal**: List users, change role, block/unblock; last-Admin protection; block revokes all refresh

**Independent Test**: Admin list/role/block; Initiator 403; blocked cannot login/refresh

### Tests for User Story 5

- [x] T046 [P] [US5] Unit test last-Admin guard in `portfolio-core-impl/src/test/java/com/portfolio/core/user/AdminUserServiceTest.java`
- [x] T047 [US5] Integration test admin list/role/block + 403 + refresh revoke in `portfolio-core-impl/src/test/java/com/portfolio/core/user/AdminUsersIntegrationTest.java` (fail first)
- [x] T048 [US5] Extend/assert US2 Initiator в†’ `GET /api/v1/admin/users` returns 403 in `RegisterIntegrationTest.java` or AdminUsersIntegrationTest

### Implementation for User Story 5

- [x] T049 [P] [US5] Add `ChangeRoleRequest`, `SetBlockedRequest` + `AdminUsersApi` in `portfolio-core-api/src/main/java/com/portfolio/core/api/admin/`
- [x] T050 [US5] Implement `AdminUserService` (list, changeRole, setBlocked, last-Admin checks, revoke-all refresh on block) in `portfolio-core-impl/src/main/java/com/portfolio/core/user/AdminUserService.java`
- [x] T051 [US5] Implement `AdminUsersController` in `portfolio-core-impl/src/main/java/com/portfolio/core/web/AdminUsersController.java`
- [x] T052 [US5] Deny login/refresh for blocked users in `AuthService.java`
- [x] T053 [US5] Make T046вЂ“T048 green

**Checkpoint**: Full role model + block behavior for coursework demo

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validation against design docs and hardening

- [ ] T054 [P] Align OpenAPI notes with implemented status codes in `specs/001-user-auth/contracts/openapi.yaml` if drift found
- [ ] T055 [P] Add multi-session integration assertion (two cookies; logout one) in `portfolio-core-impl/src/test/java/com/portfolio/core/auth/MultiSessionIntegrationTest.java`
- [ ] T056 Run full `mvn -pl portfolio-core-impl test` (or reactor `mvn test`) and fix failures
- [ ] T057 Manually walk `specs/001-user-auth/quickstart.md` scenarios AвЂ“F; note gaps in `specs/001-user-auth/quickstart.md` only if commands need path fixes
- [ ] T058 [P] Ensure no secrets committed; `.gitignore` covers `application-local.yml` if created under `portfolio-core-impl/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup** в†’ **Phase 2 Foundational** в†’ stories
- **US1 (P1)** after Foundational вЂ” MVP
- **US2** after Foundational (ideally after US1 login exists for E2E)
- **US3** after Foundational (User repo); complements US1 for demos
- **US4** after US1 (needs valid access token path)
- **US5** after US1 (+ preferably US2/US4); finishes FR-015 role/block tests
- **Polish** after desired stories complete

### User Story Dependencies

| Story | Depends on | Notes |
|-------|------------|-------|
| US1 | Phase 2 | Seed user in tests if bootstrap not ready |
| US2 | Phase 2, US1 login helpful | Independently testable via register+DB asserts |
| US3 | Phase 2 | Independent; enables demo Admin |
| US4 | US1 | Needs Bearer auth working |
| US5 | US1 | Needs Admin token (bootstrap or role update in test) |

### Parallel Opportunities

- T002/T003 after T001; T004вЂ“T006 [P]
- T007вЂ“T009, T011вЂ“T014 [P] after T010 schema exists (entities can draft in parallel with SQL)
- T024/T025 [P]; T027/T028 [P]
- T034 [P]; T043 [P]; T049 [P]
- T054/T055/T058 [P] in polish

---

## Parallel Example: User Story 1

```text
# After Phase 2:
T024 JwtTokenServiceTest.java
T025 RefreshSessionServiceTest.java
T027 LoginRequest + TokenResponse records
T028 AuthApi.java
# Then sequential:
T029 AuthService в†’ T030 AuthController в†’ T031 SecurityConfig в†’ T032 green tests
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (US1) + seeded user in tests
3. **STOP** вЂ” validate login/refresh/logout
4. Then US3 bootstrap for human demos, then US2/US4/US5

### Incremental Delivery

1. Setup + Foundational
2. US1 MVP session
3. US3 bootstrap Admin
4. US2 register
5. US4 me
6. US5 admin + block
7. Polish + quickstart AвЂ“F

### Suggested MVP scope

**Phases 1вЂ“3 only (T001вЂ“T032)** вЂ” hybrid auth session proven.

---

## Notes

- Tests included because FR-015 mandates them
- Do not implement calc/finance/UI in these tasks
- Cookie name/path/SameSite per research.md
- Format validation: all tasks use `- [ ] Tnnn ...` with paths and story labels on story phases
