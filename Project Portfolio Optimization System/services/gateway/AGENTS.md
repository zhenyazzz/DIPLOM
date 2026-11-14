# Gateway — guide for agents

Spring Boot app (`com.portfolio:portfolio-gateway`), single module, no API module.  
Package root `com.portfolio.gateway`. Port **8090**. Spring Cloud Gateway Server WebMVC (servlet stack).

## What this service is

Single entry point behind nginx for `/api/**`:

- Routes `/api/v1/auth/**` and `/api/**` → core (`CORE_URL`). Paths are forwarded **without rewriting** (no StripPrefix): core's refresh cookie is `Path=/api/v1/auth`, so the prefix must reach core unchanged
- Early JWT check via `common-security`; every other `/api/**` path needs a valid JWT
- CORS (`corsConfigurationSource`, applied by the Spring Security chain), in-memory login rate-limit per client IP
- `/api/v1/files/**` is blocked with 404: FTS is internal and reachable only from core and finance
- No business logic, no token issuance, no database

Out of scope: service discovery, distributed rate-limit (Redis), circuit breaker, routes to calc/finance/FTS.

## Package layout

| Package | Contents |
|---------|----------|
| `config` | `GatewayProperties`, `WebConfig` (Clock, CORS) |
| `filter` | `InternalPathsFilter`, `LoginRateLimitFilter` — servlet filters ordered `HIGHEST_PRECEDENCE + 10`, `+20` — both before Spring Security (`-100`), in this order |
| `web` | `ProblemResponses`, `UpstreamErrorResolver` |

Config: `src/main/resources/application.yml`. Test context: `src/test/resources/application-test.yml`.

## How to run locally

`common-security` must be installed into the local Maven repo first (from the repo root, `Project Portfolio Optimization System`):

```bash
mvn -f libs/common-security/pom.xml install
```

Then build and run with **JDK 25**:

```bash
mvn -f services/gateway/pom.xml package
java -jar services/gateway/target/gateway.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes). `CORE_URL` defaults to `http://localhost:8080`. Core must be running for proxied calls to succeed.

Container (after `mvn package` so `target/gateway.jar` exists):

```bash
docker compose -f infra/docker-compose.yml --profile apps up -d gateway
```

## Configuration

```yaml
server:
  port: ${GATEWAY_PORT:8090}
  forward-headers-strategy: framework        # behind nginx: client IP from X-Forwarded-*
spring:
  http:
    clients:
      connect-timeout: ${GATEWAY_CORE_CONNECT_TIMEOUT:2s}
      read-timeout: ${GATEWAY_CORE_READ_TIMEOUT:30s}
  cloud.gateway.server.webmvc.routes:       # core-auth: /api/v1/auth/**, core: /api/**
    uri: ${CORE_URL:http://localhost:8080}
portfolio.security.jwt.secret: ${CORE_JWT_SECRET}
gateway:
  cors.allowed-origins: ${GATEWAY_CORS_ALLOWED_ORIGINS:http://localhost:5173}
  login-rate-limit:
    capacity: ${GATEWAY_LOGIN_RATE_LIMIT_CAPACITY:5}
    window: ${GATEWAY_LOGIN_RATE_LIMIT_WINDOW:1m}
```

| Env var | Default | Purpose |
|---------|---------|---------|
| `GATEWAY_PORT` | `8090` | HTTP port |
| `CORE_URL` | `http://localhost:8080` | Upstream core |
| `CORE_JWT_SECRET` | — (required) | HS256 secret, ≥ 32 bytes; same as core |
| `GATEWAY_CORE_CONNECT_TIMEOUT` | `2s` | Connect timeout to core |
| `GATEWAY_CORE_READ_TIMEOUT` | `30s` | Read timeout to core |
| `GATEWAY_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed browser origins |
| `GATEWAY_LOGIN_RATE_LIMIT_CAPACITY` | `5` | Login attempts per IP per window |
| `GATEWAY_LOGIN_RATE_LIMIT_WINDOW` | `1m` | Fixed window length |

Permit-all (no JWT): `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /actuator/health`.

## Errors

RFC 9457 `ProblemDetail` + property `code`:

| Status | `code` | When |
|--------|--------|------|
| 401 | `UNAUTHORIZED` | missing/invalid JWT (from `common-security`) |
| 404 | `NOT_FOUND` | internal path (`/api/v1/files/**`) |
| 429 | `TOO_MANY_REQUESTS` | login limit exceeded; response has `Retry-After` |
| 502 | `UPSTREAM_UNAVAILABLE` | core unreachable |
| 504 | `UPSTREAM_TIMEOUT` | core did not answer within the read timeout |

## Do / Don't

**Do**

- Keep the permit-all list in sync with core's `portfolio.security.permit-all`
- Keep every service validating the JWT itself (defense in depth); the gateway check is an early reject only
- Forward paths as-is so core's `/api/v1/auth` cookie path keeps working
- Keep the gateway stateless apart from the in-memory rate-limit counters

**Don't**

- Add routes to FTS, calc or finance; only core calls them
- Put business logic or token issuance here
- Add `StripPrefix` (or any rewrite) on `/api/v1/auth/**`
- Trust `X-Forwarded-*` from anywhere except nginx. The leftmost `X-Forwarded-For` value becomes the client IP
  (rate-limit key), so nginx must **overwrite** it: `proxy_set_header X-Forwarded-For $remote_addr;` —
  never `$proxy_add_x_forwarded_for`, which lets a client spoof its IP and bypass the login limit
