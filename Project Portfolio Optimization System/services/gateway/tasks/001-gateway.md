# 001 — Gateway

## Цель

Единая точка входа за nginx: маршрутизация `/api/**` → core, ранняя проверка JWT, CORS, rate-limit на login,
`X-Request-Id`. Бизнес-логики и выдачи токенов нет.

## Не входит

Маршрут к FTS (`/api/v1/files/**` — внутренний), маршруты к calc/finance (их вызывает только core), service discovery,
распределённый rate-limit (Redis), circuit breaker.

## Решения

- **Spring Cloud Gateway Server WebMVC** (закрывает GW-STACK): servlet-стек, как у сервисов, поэтому `common-security`
  подключается как есть — та же проверка HS256 JWT и те же 401 `UNAUTHORIZED` Problem Details.
- Java 25, Spring Boot 4.1.1, Spring Cloud 2025.1.x (≥ 2025.1.2 — первая с поддержкой Boot 4.1). Один модуль, порт 8090.
- Пути проксируются **без переписывания** (нет StripPrefix): refresh-cookie core имеет `Path=/api/v1/auth`.
- permit-all — те же 4 auth-эндпоинта, что в core, плюс `GET /actuator/health`.
- Servlet-фильтры раньше Spring Security (-100): `RequestIdFilter` → `InternalPathsFilter` → `LoginRateLimitFilter`.
- Rate-limit — своя fixed-window реализация в памяти по IP (без Bucket4j); IP — `getRemoteAddr()` при
  `server.forward-headers-strategy: framework`.
- CORS — бин `corsConfigurationSource`, Spring Security применяет его в цепочке common-security.
- Ошибки — RFC 9457 + `code`.

## Коды ошибок

| Статус | `code` | Когда |
|--------|--------|-------|
| 401 | `UNAUTHORIZED` | нет/невалидный JWT (common-security) |
| 404 | `NOT_FOUND` | внутренний путь (`/api/v1/files/**`) |
| 429 | `TOO_MANY_REQUESTS` | превышен лимит login, есть `Retry-After` |
| 502 | `UPSTREAM_UNAVAILABLE` | core недоступен |
| 504 | `UPSTREAM_TIMEOUT` | core не ответил за read-timeout |

## Готово, когда

- `mvn verify` в `services/gateway` зелёный (unit + интеграционный тест с fake core).
- `docker compose --profile apps up` поднимает gateway рядом с core.
- `ARCHITECTURE.md` и `AGENTS.md` отражают реализацию.

## Задачи

### Фаза 0 — каркас (оркестратор)

- [x] T001 `pom.xml`, `GatewayApplication`, `GatewayProperties`, `WebConfig` (Clock, CORS), `ProblemResponses`,
  `application.yml`, `application-test.yml`, заготовки фильтров и `UpstreamErrorResolver`, `TestJwt`
- [x] T002 этот файл
- [ ] T003 `mvn verify` с context-load тестом зелёный

### Фаза 1 — реализация (параллельно)

- [ ] T010 [P] `RequestIdFilter`, `UpstreamErrorResolver` + unit-тесты
- [ ] T011 [P] `InternalPathsFilter`, `LoginRateLimitFilter` + unit-тесты
- [ ] T012 [P] `Dockerfile`, `AGENTS.md`, `ARCHITECTURE.md`, `infra/docker-compose.yml`

### Фаза 2 — интеграционный тест

- [ ] T020 `GatewayIntegrationTest`: fake core на JDK `HttpServer`, полный HTTP-сценарий

### Фаза 3 — проверка (оркестратор)

- [ ] T030 `mvn verify` зелёный, ревью, правки
