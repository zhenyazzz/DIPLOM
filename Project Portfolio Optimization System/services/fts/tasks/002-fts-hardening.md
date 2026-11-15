# 002 — FTS hardening (по итогам ревью PR #2)

## Цель

Убрать недочёты из ревью FTS, не расширяя функциональность.

## Решения

- **Доступ к файлам (D1).** FTS — внутренний сервис: браузер в него не ходит, gateway отдаёт 404 на `/api/v1/files/**`.
  Права на доменные объекты (импорт, отчёт, вложение) проверяет domain/finance, затем выдаёт браузеру presigned URL от FTS.
  Внутри FTS: читать может любой валидный JWT (domain/finance с токеном пользователя), удалять — владелец или `ADMIN`.
- **Владелец (D2).** Явная колонка `owner_id UUID` из `SecuritySupport.requireUserId()`; в API `createdBy` → `ownerId: UUID`.
  Ownership больше не зависит от формата `JwtAuditorAware`.
- **Транзакции (D3).** `upload` без транзакции вокруг сетевой загрузки; транзакция только на `save`.
  Если `save` упал — объект удаляется из MinIO (best-effort, ошибка удаления логируется и не маскирует исходную).
- **Проверка содержимого (D4).** Сигнатура первых байт сверяется с заявленным типом, без Tika:
  PDF `%PDF-`, PNG `89 50 4E 47 0D 0A 1A 0A`, JPEG `FF D8 FF`, OOXML (xlsx/docx) `PK 03 04`,
  текст (`text/csv`, `text/plain`) — первые 8 КБ без байта `00` и валидный UTF-8 (обрезанный последний символ допустим).
  Несовпадение → 415 `CONTENT_MISMATCH`.
- **Ошибка чтения загрузки (D5).** `IOException` при чтении multipart → 400 `UPLOAD_READ_FAILED` (Problem Details).
- **Контракт (D6).** Интеграционные тесты валидируют каждый ответ MockMvc по `contracts/openapi/fts-v1.yaml`
  (`com.atlassian.oai:swagger-request-validator-mockmvc`). Если библиотека не поддерживает OpenAPI 3.1 — перевести yaml на 3.0.3.

## Готово, когда

- `mvn verify` в `services/fts` зелёный, тесты не пропущены.
- Каждый сценарий ошибки — отдельный тест.
- Ответы API в интеграционных тестах проходят валидацию по `fts-v1.yaml`.
- `ARCHITECTURE.md` и `AGENTS.md` отражают D1.

## Задачи

### Фаза 1 — реализация (параллельно, файлы не пересекаются)

- [x] T101 [P] Сервис и хранилище (D2, D3, D5 + вызов D4):
  `V2__add_owner_id.sql` (колонка, backfill из `created_by::uuid`, `NOT NULL`, индекс), `FileEntity` (`ownerId`, `isOwnedBy(UUID)`),
  `FileResponse.ownerId`, `FileMapper`, `FileService`, `ObjectStorage.delete` + `MinioObjectStorage`, `FtsException` (новые коды),
  вызов `ContentSignatureVerifier.matches(contentType, head)` по интерфейсу из T102.
- [x] T102 [P] `file/ContentSignatureVerifier` (D4) + `ContentSignatureVerifierTest` — чистый класс без Spring-зависимостей,
  сигнатура `boolean matches(String contentType, byte[] head)`, `static final int HEAD_SIZE = 8192`.
- [x] T103 [P] Документы (D1, D2, D4, D5): `contracts/openapi/fts-v1.yaml` (`ownerId`, `CONTENT_MISMATCH`, `UPLOAD_READ_FAILED`),
  `docs/architecture/ARCHITECTURE.md` (FTS внутренний, маршрут gateway убран, скачивание через presigned URL от domain/finance),
  `services/fts/AGENTS.md`.

### Фаза 2 — тесты (параллельно)

- [x] T201 [P] `FileServiceTest`: ownerId, откат объекта при падении `save`, `UPLOAD_READ_FAILED`, `CONTENT_MISMATCH`.
- [x] T202 [P] `FilesApiIntegrationTest`: разбить комбинированный тест на отдельные, добавить `CONTENT_MISMATCH`,
  подключить валидацию ответов по `fts-v1.yaml` (D6) в базовом `IntegrationTest`.

### Фаза 3 — проверка (оркестратор)

- [x] T301 ревью, `mvn verify`, compose-проверка, коммит в ветку PR #2.
