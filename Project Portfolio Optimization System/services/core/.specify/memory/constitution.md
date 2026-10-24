<!--
Sync Impact Report
- Version change: (none / template) → 1.0.0
- Modified principles: N/A (initial ratification from placeholders)
- Added sections:
  - Core Principles I–VI (Domain-first, Money safety, Explicit integration,
    Test pyramid, Simplicity, Modular Maven API/Impl)
  - Technology Stack & Boundaries
  - Roles & Quality Gates
  - Governance
- Removed sections: template placeholder comments only
- Follow-up TODOs:
  - Pin exact Maven coordinates (Spring Boot patch, MapStruct, JDK) at project bootstrap
  - Detailed code-style / layering conventions may be amended later after discussion
-->

# Portfolio Core Service Constitution

## Core Principles

### I. Domain-First Core
Core owns the project registry, evaluation criteria, portfolio scenarios,
users/roles, portfolio budget, and orchestration of specialized operations.
Core MUST NOT implement SAW scoring or 0-1 knapsack optimization locally;
those MUST be delegated to calc-service over HTTP.
Financial file parsing (CSV/XLSX and related metrics extraction) belongs to
finance-service and MAY remain a stub until a later iteration.
Rationale: keeps domain state authoritative in one place and isolates math and
file IO behind explicit service boundaries required by the coursework.

### II. Money & Weights Safety
All monetary amounts, criterion weights, normalized scores, and optimization
inputs/outputs that represent money or weights MUST use `java.math.BigDecimal`.
`double` and `float` MUST NOT be used for money or weights.
Rationale: floating-point drift is unacceptable for budget and MCDA decisions.

### III. Explicit Integration
Calls from core to calc-service and finance-service MUST use a versioned REST
JSON contract, configured timeouts, and mapped, actionable error responses.
If calc-service is unavailable or returns an error, core MUST fail visibly
with a clear API error; silent success or empty portfolio results are forbidden.
Rationale: integration between components is a graded requirement and must be
demonstrable and debuggable.

### IV. Test Pyramid
Every feature is incomplete until covered by:
- unit tests for domain rules and validation;
- integration tests for REST API, PostgreSQL/Flyway, and the calc-service contract;
- UI tests live in the frontend project and are out of core's build, but
  core MUST expose stable contracts suitable for them.
A feature MUST NOT be considered done without a green relevant test set.
Rationale: unit + integration + UI coverage is mandatory for the assignment.

### V. Simplicity for Coursework
No Kafka, Kubernetes, enterprise service bus, or other infra that is not
required to meet functional goals.
Persistence is PostgreSQL with Flyway migrations.
Inside core, structure is a Spring Boot modular layout (Maven modules),
not a distributed mesh.
YAGNI applies: do not add abstractions without a concrete feature need.
Rationale: delivery risk and demo reliability matter more than enterprise theater.

### VI. Modular Maven API / Impl
Core MUST be a multi-module Maven project with a parent POM and at least:
- `api` — DTOs as Java `record` types, controller/API interfaces, Bean Validation
  annotations, and other contract-facing types only;
- `impl` — Spring Boot application, domain/services, persistence, MapStruct
  mappers, security, and implementations of `api` contracts.

MapStruct MUST be used for mapping between API records and internal models.
Hand-written repetitive mapping code SHOULD be avoided unless MapStruct cannot
express the case cleanly.
Code MUST stay minimal, explicit, and carefully structured: no speculative
layers, no unused indirection, no "future-proof" noise.
Finer style conventions MAY be refined later via constitution amendment;
until then, prefer the smallest clear design that satisfies the feature spec.
Rationale: stable contracts in `api` keep frontend/clients honest; `impl` stays
replaceable; records + MapStruct reduce boilerplate without sacrificing clarity.

## Technology Stack & Boundaries

- Language: newest stable JDK supported by the chosen Spring Boot line
  (prefer current LTS; at ratification target Java 25 LTS, compatible up to
  the Boot-supported ceiling).
- Framework: latest stable Spring Boot 4.1.x patch at bootstrap time,
  with Spring Security.
- API style: REST + JSON for the frontend.
- Database: PostgreSQL + Flyway (migrations owned by `impl`).
- calc-service: stateless HTTP service for SAW scoring and 0-1 knapsack
  portfolio selection under budget constraints.
- finance-service: deferred; stub or absent client is acceptable until specified.
- Exact dependency versions MUST be pinned in the parent POM when the Maven
  skeleton is created (not guessed ad hoc per feature).

## Roles & Quality Gates

Minimum roles supported by core authorization:
- Initiator
- Expert / Analyst
- Decision Maker
- Admin

Quality gates before calling a feature done:
1. Spec/plan/tasks (Spec Kit) exist and were reviewed.
2. Constitution principles above are not violated.
3. Required tests are present and green.
4. Public REST contracts live in `api`; behavior lives in `impl`.

## Governance

This constitution supersedes local habits, tutorial defaults, and ad-hoc agent
preferences when they conflict.
Amendments MUST go through `/speckit-constitution` with a semantic version bump:
- MAJOR: remove or redefine a principle incompatibly;
- MINOR: add a principle/section or materially expand guidance;
- PATCH: clarifications and non-semantic wording fixes.

Compliance review is expected at plan and implement time: if a change would
break Domain-First Core, Money Safety, Explicit Integration, Test Pyramid,
Simplicity, or Modular API/Impl, it MUST be redesigned or the constitution
amended first.
Runtime feature work proceeds via Spec Kit (`/speckit-specify` → plan → tasks
→ implement), not by silently expanding scope inside constitution updates.

**Version**: 1.0.0 | **Ratified**: 2026-10-18 | **Last Amended**: 2026-10-18
