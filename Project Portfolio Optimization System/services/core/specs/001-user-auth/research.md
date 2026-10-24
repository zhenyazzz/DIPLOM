# Research: 001-user-auth

## R1 — Access token format

**Decision**: Signed JWT (HS256) as access credential in JSON body; claims: `sub` (user UUID), `role`, `email`, `iat`, `exp`. Symmetric secret from config (`CORE_JWT_SECRET`, min 32 bytes).

**Rationale**: Fits hybrid model; Spring Security can validate Bearer JWT without a session store; coursework-friendly.

**Alternatives considered**:
- Opaque access + server lookup — more DB hits; rejected for simplicity.
- RSA/JWKS — overkill for single core service.
- Spring Authorization Server — too heavy for this feature.

## R2 — Refresh token format

**Decision**: Opaque random token (32+ bytes, URL-safe Base64). Persist only SHA-256 hash in `refresh_sessions`. Cookie carries raw value once.

**Rationale**: Easy revoke/rotate; theft of DB hashes alone is not enough to renew; matches FR-009.

**Alternatives considered**:
- Refresh as JWT — harder to revoke without denylist; rejected.
- Store raw refresh in DB — unnecessary secret exposure; rejected.

## R3 — Cookie attributes

**Decision**:
- Name: `refresh_token`
- `HttpOnly=true`, `Path=/api/v1/auth`, `SameSite=Lax`
- `Secure=true` when `CORE_COOKIE_SECURE=true` (prod/HTTPS); `false` for local HTTP
- Max-Age aligned with refresh TTL (14 days)

**Rationale**: Narrow path limits cookie send surface; SameSite=Lax reduces CSRF on top-level navigations; local Secure=false keeps Vite/dev usable.

**Alternatives considered**:
- `SameSite=None; Secure` — needed only for true cross-site frontends; defer until deploy topology requires it.
- Path=/ — broader CSRF/theft surface; rejected.

## R4 — TTL values

**Decision**: Access = 15 minutes; Refresh = 14 days. Configurable via `application.yml`.

**Rationale**: Matches clarified ~10–15 min residual window after block; 14 days is comfortable for demo without daily re-login.

## R5 — Password hashing

**Decision**: Spring Security `BCryptPasswordEncoder` (strength 10 default).

**Rationale**: Standard, well-supported; Argon2 optional later, not required for v1.

## R6 — JWT library

**Decision**: Use Nimbus JOSE + JWT via Spring Security OAuth2 Resource Server / `spring-boot-starter-oauth2-resource-server` for validation, and a thin issuer component (Nimbus or jjwt) for creating tokens — prefer **one stack: Nimbus** for both issue and validate to avoid two libraries.

**Rationale**: Aligns with Spring Boot 4.x security stack; fewer dependencies.

**Alternatives considered**: jjwt alone — fine but less native to Spring Security filter chain.

## R7 — Maven module layout (bootstrap this feature)

**Decision**: Create multi-module Maven under `services/core`:
- `portfolio-core` (parent POM)
- `portfolio-core-api` — records, validation, controller interfaces
- `portfolio-core-impl` — Boot app, JPA, security, MapStruct, Flyway

**Rationale**: Constitution VI; this feature is the first code in core, so skeleton lands here.

## R8 — Persistence & tests

**Decision**: Spring Data JPA + Flyway; integration tests with Testcontainers PostgreSQL; `@SpringBootTest` + MockMvc/WebTestClient.

**Rationale**: Constitution storage + test pyramid; Testcontainers matches real SQL.

**Alternatives considered**: H2 for tests — rejected (Postgres-specific types/behavior drift).

## R9 — Bootstrap Admin

**Decision**: `ApplicationRunner` (or `@PostConstruct` service) on startup: if `users` count = 0 and `CORE_BOOTSTRAP_ADMIN_EMAIL` + `CORE_BOOTSTRAP_ADMIN_PASSWORD` are set, create Admin. If users exist, no-op. No public HTTP bootstrap endpoint (avoids accidental open admin creation).

**Rationale**: Spec allows “configuration/seed”; env-based seed is safer than a forever-open API.

**Alternatives considered**:
- `POST /auth/bootstrap` while empty — convenient but easier to misuse; rejected for v1.
- SQL seed in Flyway — passwords in VCS risk; rejected.

## R10 — CSRF

**Decision**: For v1, rely on `SameSite=Lax` + refresh cookie path limited to `/api/v1/auth`; no double-submit CSRF token. Document that mutating auth endpoints should not be triggered by cross-site form posts in browsers respecting SameSite.

**Rationale**: Coursework simplicity; full CSRF tokens deferred if frontend is cross-site with `SameSite=None`.

## R11 — Role encoding

**Decision**: Enum `UserRole`: `INITIATOR`, `EXPERT_ANALYST`, `DECISION_MAKER`, `ADMIN`. API JSON uses these strings. Spring authority: `ROLE_INITIATOR`, etc.

**Rationale**: Single role per user; Expert/Analyst is one value per spec.

## R12 — API versioning

**Decision**: Prefix `/api/v1/...`.

**Rationale**: Constitution “versioned REST”; leaves room for breaking changes later.

## R13 — MapStruct scope this feature

**Decision**: MapStruct mappers for `User` entity ↔ `UserResponse` (and list items). Auth request records need no entity mapping beyond service construction.

**Rationale**: Constitution VI without busywork mappers.

## R14 — Calc / finance

**Decision**: No clients, no stubs required in this feature.

**Rationale**: Out of scope; Explicit Integration gate N/A until those calls exist.
