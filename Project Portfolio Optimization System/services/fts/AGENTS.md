# FTS — guide for agents

Spring Boot app (`com.portfolio:fts-impl`), API module `com.portfolio:fts-api`.  
Package root `com.portfolio.fts`. Port **8083**. Only wrapper over MinIO in this system.

## What this service is

File transfer for Portfolio:

- Metadata in PostgreSQL schema `fts` (same instance as core); bytes in MinIO
- Buckets `IMPORTS` / `REPORTS` / `ATTACHMENTS` → `fts-imports`, `fts-reports`, `fts-attachments` (created on start)
- Object key = UUID; client filename is metadata only; sha256 hashed on upload
- JWT validate-only via `common-security`; `common-web` only for `BaseEntity` (no CRUD kit)
- Presigned URLs signed against `fts.minio.public-endpoint`

Out of scope: versioning, sharing, per-file ACL, antivirus, chunked multipart upload, direct MinIO access from other services.

## Package layout

| Module | Package | Contents |
|--------|---------|----------|
| `fts-api` | `com.portfolio.fts.api` | `FilesApi`, `FileResponse`, `DownloadUrlResponse`, `FileBucket` |
| `fts-impl` | `com.portfolio.fts.config` | `FtsProperties` |
| `fts-impl` | `com.portfolio.fts.storage` | MinIO client, `ObjectStorage`, bucket init |
| `fts-impl` | `com.portfolio.fts.file` | Flyway, entity, repository, mapper, service, controller |
| `fts-impl` | `com.portfolio.fts.error` | RFC 9457 Problem Details + `code` |

Contract: `contracts/openapi/fts-v1.yaml`. Keep API types and the YAML in sync.

## How to run locally

From the repo root (`Project Portfolio Optimization System`):

```bash
docker compose -f infra/docker-compose.yml up -d
```

Starts postgres + MinIO. Then build and run with **JDK 25**:

```bash
mvn -f services/fts/pom.xml package
java -jar services/fts/fts-impl/target/fts.jar
```

Needs `CORE_JWT_SECRET` (≥ 32 UTF-8 bytes). Defaults talk to `localhost:5432` and `localhost:9000`.

Container (after `mvn package` so `fts-impl/target/fts.jar` exists):

```bash
docker compose -f infra/docker-compose.yml --profile apps up -d fts
```

## Configuration

```yaml
fts:
  download-url-ttl: 5m
  minio:
    endpoint: ${MINIO_ENDPOINT}                 # in-cluster, e.g. http://minio:9000
    public-endpoint: ${MINIO_PUBLIC_ENDPOINT}   # browser-visible, e.g. http://localhost:9000
    access-key: ${MINIO_ACCESS_KEY}
    secret-key: ${MINIO_SECRET_KEY}
    region: us-east-1
  buckets:
    imports:      { name: fts-imports,      max-size: 20MB, content-types: [text/csv, ...] }
    reports:      { name: fts-reports,      max-size: 20MB, content-types: [application/pdf, ...] }
    attachments:  { name: fts-attachments,  max-size: 50MB, content-types: [...] }
```

JWT: `portfolio.security.jwt.secret` ← `CORE_JWT_SECRET`. Datasource: `SPRING_DATASOURCE_*` (schema `fts` via Flyway).

## Errors

RFC 9457 `ProblemDetail` + property `code`:

| Status | `code` |
|--------|--------|
| 404 | `FILE_NOT_FOUND` |
| 400 | `EMPTY_FILE`, `BAD_REQUEST` |
| 415 | `CONTENT_TYPE_NOT_ALLOWED` |
| 413 | `FILE_TOO_LARGE` |
| 403 | `FORBIDDEN` |
| 503 | `STORAGE_UNAVAILABLE` |
| 401 | `UNAUTHORIZED` (from `common-security`) |

Delete: uploader or `ADMIN`. Read: any authenticated user. Soft-deleted files are 404.

## Do / Don't

**Do**

- Talk to MinIO only from this service; other services use `fileId`
- Treat files as immutable (new version = new id)
- Soft-delete (`deleted_at`); never rewrite object bytes
- Enforce per-bucket `max-size` and content-type allow-list

**Don't**

- Give other services MinIO credentials or endpoints
- Version, share, or ACL individual objects here
- Introduce `BigDecimal` — FTS stores bytes and metadata, not money
