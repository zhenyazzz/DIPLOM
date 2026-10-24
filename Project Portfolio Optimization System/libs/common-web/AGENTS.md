# common-web — guide for agents

Maven jar (`com.portfolio:common-web:0.1.0-SNAPSHOT`), package root `com.portfolio.common.web`.  
Not a Spring Boot application. Not a Boot autoconfig starter.

Reference implementation (copy patterns from here):  
`src/test/java/com/portfolio/common/web/support/` (`Project*` + `TestApplication`).

## What this library is

Reusable REST/JPA CRUD kit:

- `BaseEntity` — UUID id, audit columns, `@Version`
- `AbstractJpaCrudService` / `AbstractCrudController` — CRUD + paged RSQL search
- `BaseRepository` — `JpaRepository` + `JpaSpecificationExecutor`
- `EntityMapper` — mapping SPI (MapStruct lives in the consumer)
- `@EnableCommonWeb` — Pageable support, `CommonWebProperties`, optional Problem Details advice

Out of scope: JWT/auth, Flyway, OpenAPI, nested/join RSQL (`author.name`), Boot starter auto-wiring.

Security / `AuditorAware` → use `libs/common-security` (or consumer-owned bean), not this jar.

## How enable works

`@EnableCommonWeb` → `CommonWebImportSelector` imports:

1. Always: `CommonWebConfiguration` (`@EnableSpringDataWebSupport` + `CommonWebProperties` bean)
2. If `problemDetails = true` (default): `CommonWebProblemDetailsHandler`

CRUD itself is **not** auto-registered. Consumer extends abstract classes and declares `@RestController` / `@Service` as usual.

```java
@SpringBootApplication
@EnableCommonWeb                          // or @EnableCommonWeb(problemDetails = false)
@EnableJpaAuditing                        // required for BaseEntity audit timestamps / by-fields
public class App { }

@Bean
AuditorAware<String> auditorAware() { ... } // consumer or common-security — NOT provided by common-web
```

If the app already has a global `@ControllerAdvice` that should own kit exceptions:

```java
@EnableCommonWeb(problemDetails = false)
```

Then map `NotFoundException` and `FilterBadRequestException` yourself (prefer RFC 7807 `ProblemDetail`).

## Consumer checklist (standard resource)

1. Depend on `com.portfolio:common-web`
2. `@EnableCommonWeb` + `@EnableJpaAuditing` + `AuditorAware<String>`
3. Entity `extends BaseEntity`
4. Repository `extends BaseRepository<Entity>`
5. Mapper `implements EntityMapper<Entity, Create, Update, Response>` (null-ignore on update)
6. Service `extends AbstractJpaCrudService<Entity, Create, Update, Response>` + `FieldAllowList`
7. Controller `extends AbstractCrudController<Create, Update, Response>` + `@RequestMapping`

### Minimal shapes

```java
@Entity
@Table(name = "projects")
public class ProjectEntity extends BaseEntity { /* domain fields */ }

public interface ProjectRepository extends BaseRepository<ProjectEntity> {}

@Service
public class ProjectService
        extends AbstractJpaCrudService<ProjectEntity, CreateProjectRequest, UpdateProjectRequest, ProjectResponse> {
    public ProjectService(ProjectRepository repo, ProjectMapper mapper, CommonWebProperties props) {
        super(repo, mapper, FieldAllowList.of(
                FieldRule.filterAndSort("name", String.class),
                FieldRule.filterAndSort("status", ProjectStatus.class),
                FieldRule.filterAndSort("budget", BigDecimal.class)
        ), props);
    }
}

@RestController
@RequestMapping("/api/projects")
public class ProjectController
        extends AbstractCrudController<CreateProjectRequest, UpdateProjectRequest, ProjectResponse> {
    public ProjectController(ProjectService service) {
        super(service);
    }
}
```

Do **not** put Entity/Repository type parameters on the controller — only Create / Update / Response.

## HTTP contract

| Method | Path | Status |
|--------|------|--------|
| POST | `/` | 201 |
| GET | `/{id}` | 200 / 404 |
| PUT | `/{id}` | 200 / 404 |
| DELETE | `/{id}` | 204 / 404 |
| GET | `/` | 200 — Spring `Pageable` (`page`, `size`, `sort=field,asc`) + `filter` (RSQL) |

Response list body: `PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`).

Max page size clamped via `CommonWebProperties` (default max 100). Override with a `@Bean CommonWebProperties` if needed.

## RSQL / allow-list rules

- Query param: `filter` (example: `status==ACTIVE;budget=ge=1000`)
- Operators: `==`, `!=`, `=gt=`, `=ge=`, `=lt=`, `=le=`, `=in=`, `=out=`, `=like=`, `=ilike=`
- Logic: `;` AND, `,` OR, parentheses
- **Root entity fields only** — nested paths → 400
- Only fields in `FieldAllowList` are filterable/sortable; unknown → 400 `BAD_FILTER`
- Never allow-list audit/sensitive columns (`createdBy`, passwords, etc.) unless explicitly required
- Filtering runs in DB via JPA `Specification` — do not load full tables in memory

## Mapping rules

- Create DTO ≠ Update DTO ≠ Response DTO
- Update: ignore nulls (partial-friendly PUT); do not map `id` / audit fields from client DTOs
- Prefer MapStruct in the consumer with `NullValuePropertyMappingStrategy.IGNORE` on update
- Kit only sees `EntityMapper`

## Errors

Kit advice (when enabled) returns RFC 7807 `ProblemDetail` with property `code`:

- `NOT_FOUND` — missing id
- `BAD_FILTER` — bad RSQL / sort field / type

Validation (`@Valid`) is **not** handled by the kit — Boot / app advice owns it. Enable `spring.mvc.problemdetails.enabled=true` in consumers if you want Problem Details for validation too.

## Do / Don't

**Do**

- Extend kit bases for ordinary CRUD resources
- Keep custom endpoints (auth, files, orchestration) as separate controllers/services
- Override service hooks by subclass methods when business rules differ; keep using kit for happy-path CRUD
- Copy patterns from test `support/Project*`

**Don't**

- Turn this module into `@SpringBootApplication`
- Depend on `services/*` from this jar
- Add Spring Security / JWT here (use `common-security`)
- Reintroduce dual JSON+RSQL filters or nested join filters without an explicit product decision
- Auto-import a second advice that also handles `MethodArgumentNotValidException` if the app already has one — use `problemDetails = false`
- Hand-write `findByXAndY` for combinations the allow-list + RSQL already covers

## Build / test

JDK 25. From this module:

```bash
mvn test
```

## Maven coordinate

```xml
<dependency>
    <groupId>com.portfolio</groupId>
    <artifactId>common-web</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
