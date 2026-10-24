# Data Model: 001-user-auth

## Entities

### User

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, generated |
| email | VARCHAR(320) | UNIQUE, NOT NULL, case-normalized to lower |
| display_name | VARCHAR(120) | NOT NULL, 1–120 chars |
| password_hash | VARCHAR(255) | NOT NULL (BCrypt) |
| role | VARCHAR(32) / enum | NOT NULL; one of INITIATOR, EXPERT_ANALYST, DECISION_MAKER, ADMIN |
| blocked | BOOLEAN | NOT NULL, default false |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Validation (app layer)**:
- email: valid format, unique
- password (input only): min 8 characters; never persisted plaintext
- display_name: required, trimmed, non-blank

**Rules**:
- Self-registration always sets `role=INITIATOR`, `blocked=false`
- Bootstrap creates exactly one Admin when table empty
- Cannot block or demote the last remaining Admin (`role=ADMIN` AND `blocked=false`)

### RefreshSession

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK → User.id, NOT NULL, ON DELETE CASCADE |
| token_hash | CHAR(64) or BYTEA | NOT NULL, UNIQUE (SHA-256 hex of raw token) |
| expires_at | TIMESTAMPTZ | NOT NULL |
| revoked_at | TIMESTAMPTZ | NULL if active |
| created_at | TIMESTAMPTZ | NOT NULL |
| replaced_by_id | UUID | NULL, FK → RefreshSession.id (optional audit of rotation) |

**Rules**:
- Active session: `revoked_at IS NULL` AND `expires_at > now()`
- Login creates a new row; logout sets `revoked_at`
- Refresh rotation: revoke old, insert new (same user)
- Block user: revoke all sessions for `user_id`
- Multiple active rows per user allowed

### Access credential (not a table)

JWT only; not persisted. Claims: `sub`, `role`, `email`, `iat`, `exp`.

## Relationships

```text
User 1 ─── * RefreshSession
```

## State transitions

### User.blocked

```text
[active] --Admin block--> [blocked]
[blocked] --Admin unblock--> [active]
```

On → blocked: revoke all RefreshSession for user; deny login/refresh.

### RefreshSession

```text
[active] --logout / rotate / block / expiry--> [revoked or expired]
```

Expired treated as invalid same as revoked for renewal.

### Role

```text
Any role --Admin assign--> Any of {INITIATOR, EXPERT_ANALYST, DECISION_MAKER, ADMIN}
```

Constraint: operation rejected if it would leave zero active Admins.

## Indexes

- `users(email)` UNIQUE
- `refresh_sessions(token_hash)` UNIQUE
- `refresh_sessions(user_id)`
- `refresh_sessions(expires_at)` optional for cleanup jobs (future)

## Flyway

- `V1__users_and_refresh_sessions.sql` — create both tables + indexes
