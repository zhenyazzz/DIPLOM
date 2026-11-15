# common-security — guide for agents

Maven jar (`com.portfolio:common-security:0.1.0-SNAPSHOT`), package root `com.portfolio.common.security`.  
Spring Boot **auto-config starter** (validate-only JWT resource server). Not a Spring Boot application. Not a token issuer.

Reference harness: `src/test/java/com/portfolio/common/security/support/`.

## What this library is

Reusable JWT **validation** kit for Portfolio servlet services:

- Auto-config via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- HS256 `JwtDecoder` (shared secret; claims: `sub`, `roles`[] and/or `role`, `email`)
- Default `SecurityFilterChain`: `permit-all` entries + `anyRequest().authenticated()`, CSRF off, stateless
- `@EnableMethodSecurity` for `@PreAuthorize` / `@Secured`
- Problem Details 401 (`UNAUTHORIZED`) / 403 (`FORBIDDEN`) entry-point and access-denied handlers
- `SecuritySupport.requireUserId()` / `requireRole()` / `requireRoles()` / `hasRole()` (fail-fast)
- `AccessTokenVerifier` + `AccessTokenClaims` for non-HTTP verification
- `JwtAuditorAware` (`AuditorAware<String>`) when missing

Out of scope: token issuance/signing, refresh cookies, user CRUD, JWKS/issuer-uri, path→role YAML maps.

## How activation works

Add the dependency and set `portfolio.security.jwt.secret` (≥ 32 UTF-8 bytes). No `@Enable…` annotation.

Blank/short secret → **fail-fast** at context startup.

Beans are registered with `@Bean` in `PortfolioSecurityAutoConfiguration` (not `@Component` / `@Service` on helper classes). That is intentional for Boot starters: the jar is not component-scanned, conditions (`@ConditionalOnMissingBean`, `filter-chain.enabled`) stay in one place, and consumers can override any bean.

### Boot 4.1 coexistence

Library auto-config runs **before** Boot’s `OAuth2ResourceServerWebSecurityAutoConfiguration`. Providing our `SecurityFilterChain` makes Boot’s default JWT chain back off (`@ConditionalOnMissingBean(SecurityFilterChain)` via `@ConditionalOnDefaultWebSecurity`). Do not register a second catch-all chain unless you use `securityMatcher` + `@Order`.

## Configuration

```yaml
portfolio:
  security:
    jwt:
      secret: ${CORE_JWT_SECRET}   # required, ≥ 32 UTF-8 bytes
      role-claim: role             # optional, default role
    permit-all:                    # optional, METHOD:pathPattern
      - POST:/api/v1/auth/login
      - POST:/api/v1/auth/register
    filter-chain:
      enabled: true                # false → consumer owns SecurityFilterChain
```

Role claim(s) → authorities `ROLE_{code}` (supports `roles` array and single `role`).

## Consumer checklist

1. Depend on `com.portfolio:common-security`
2. Set `portfolio.security.jwt.secret` (and permit-all for public auth routes if any)
3. Protect privileged operations with method annotations, e.g. `@PreAuthorize("hasRole('ADMIN')")`
4. Optionally inject `SecuritySupport` / `AccessTokenVerifier` / rely on `AuditorAware` for JPA auditing
5. Keep **issuance** in **identity** (`JwtTokenService`) — do not add sign/issue APIs here

### Opt out of default filter chain

```yaml
portfolio:
  security:
    filter-chain:
      enabled: false
```

Library still supplies `JwtDecoder`, converter, helpers, and auditor (unless overridden). Consumer declares `SecurityFilterChain`.

## Programmatic API

| Type | Behavior |
|------|----------|
| `SecuritySupport.requireUserId()` | Current JWT `sub` as `UUID`; else `UnauthenticatedException` |
| `SecuritySupport.requireRole()` | Role claim (no `ROLE_` prefix); else `UnauthenticatedException` |
| `AccessTokenVerifier.verify(rawJwt)` | Decode/validate; returns `AccessTokenClaims`; invalid → `IllegalArgumentException` |

## Errors

Filter-chain auth failures return Problem Details JSON:

| Status | `code` | When |
|--------|--------|------|
| 401 | `UNAUTHORIZED` | Missing/invalid/expired token on protected route |
| 403 | `FORBIDDEN` | Authenticated but method security denies |

Override by providing your own `AuthenticationEntryPoint` / `AccessDeniedHandler` beans.

## Do / Don't

**Do**

- Add dependency + `portfolio.security.*` config
- Use `@PreAuthorize` for role checks
- Share the same HS256 secret the issuer uses
- Override beans with `@ConditionalOnMissingBean` targets when customizing

**Don't**

- Move token issuance into this jar
- Depend on `services/*` from this module
- Expect path→role rules in YAML (use method security)
- Register a conflicting catch-all `SecurityFilterChain` alongside the library default without opt-out

## Build / test

JDK 25. From this module:

```bash
mvn test
```

## Maven coordinate

```xml
<dependency>
    <groupId>com.portfolio</groupId>
    <artifactId>common-security</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
