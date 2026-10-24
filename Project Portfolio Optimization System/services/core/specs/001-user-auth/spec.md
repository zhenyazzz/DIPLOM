# Feature Specification: User Accounts & Session Auth

**Feature Branch**: `001-user-auth`

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "User accounts, roles, JWT access + refresh-cookie auth для core — регистрация/логин/логаут, me/profile, admin роли и блокировка; без OAuth, проектов, calc/finance и UI."

## Clarifications

### Session 2026-10-18

- Q: Какую модель входа и продления сессии фиксируем для этой фичи как канон? → A: Hybrid — short-lived access in response body + refresh in HttpOnly cookie (Option A)
- Q: Сколько одновременных сессий (устройств/браузеров) разрешаем одному пользователю? → A: Many concurrent refresh sessions; logout revokes current only; block revokes all refresh sessions (Option A)
- Q: Как система должна мгновенно отсекать уже выданный access у заблокированного пользователя? → A: Coursework-simple: rely on short access TTL (~10–15 min); on block revoke all refresh sessions and deny new sign-in/renewal; no mandatory per-request ban check or ban-cache in v1

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in and stay signed in safely (Priority: P1)

A registered user signs in with email and password. The system returns a short-lived access credential in the response body and establishes a long-lived refresh credential that the browser stores in a cookie which page scripts cannot read. While the access credential is valid, the user can call protected operations. When it expires, the client renews access using the refresh cookie without asking for the password again. The user can sign out; after that, renewal must fail.

**Why this priority**: Without reliable sign-in / renew / sign-out, no later portfolio feature can be authorized.

**Independent Test**: Create a user (via bootstrap or registration), sign in, call "who am I", wait for/force access expiry, renew, sign out, confirm renew fails.

**Acceptance Scenarios**:

1. **Given** a known active user with a correct password, **When** they sign in, **Then** they receive a short-lived access credential in the response body and a refresh credential is set only as an HttpOnly cookie (not readable by page scripts), and they can load their own profile.
2. **Given** a valid refresh cookie and an expired or missing access credential, **When** the client requests renewal, **Then** a new access credential is issued and the previous refresh credential is rotated (old one no longer works).
3. **Given** a signed-in user with possibly other active devices, **When** they sign out on this client, **Then** only this client’s refresh credential is revoked server-side and its cookie cleared; renewal on this client fails unauthorized, while other devices’ refresh sessions remain usable until separately signed out, expired, or the user is blocked.
4. **Given** wrong email or wrong password, **When** sign-in is attempted, **Then** the system responds unauthorized with the same generic failure shape (no “email exists / does not exist” distinction).

---

### User Story 2 - Self-register as Initiator (Priority: P2)

A new person creates an account with email, display name, and password. The account is created with the Initiator role only. They can immediately sign in. Registration does not grant Admin or other elevated roles.

**Why this priority**: Needed to populate the system with ordinary users without manual DB edits; still secondary to login itself.

**Independent Test**: Register a new email, sign in, confirm role is Initiator only; attempt to call an admin-only action and get forbidden.

**Acceptance Scenarios**:

1. **Given** an unused valid email and a password meeting policy, **When** the person registers, **Then** an active Initiator account is created and sign-in works.
2. **Given** an email already registered, **When** registration is attempted, **Then** the system rejects the request with a clear validation/conflict error (without creating a second account).
3. **Given** a freshly registered Initiator, **When** they call an admin-only operation, **Then** access is forbidden.

---

### User Story 3 - Bootstrap first Admin (Priority: P2)

On a fresh empty system, exactly one initial Admin account can be created through a documented bootstrap path (configuration/seed for empty database). After at least one Admin exists, that bootstrap path must refuse to create another Admin. Further Admins are created only by an existing Admin changing a user’s role.

**Why this priority**: Coursework and demos need a deterministic privileged account without leaving open “register as Admin”.

**Independent Test**: Start with empty user store → bootstrap Admin succeeds → second bootstrap fails → Admin can promote another user.

**Acceptance Scenarios**:

1. **Given** no users exist, **When** bootstrap Admin is executed with configured credentials, **Then** one Admin account exists and can sign in.
2. **Given** at least one user already exists, **When** bootstrap is attempted again, **Then** it is rejected and no additional bootstrap Admin is created.

---

### User Story 4 - View my profile (Priority: P3)

A signed-in user retrieves their own profile: stable id, email, display name, role, active/blocked status.

**Why this priority**: Small but required for any client shell and for verifying auth in demos/tests.

**Independent Test**: Sign in → request profile → fields match; without credentials → unauthorized.

**Acceptance Scenarios**:

1. **Given** a valid access credential, **When** the user requests their profile, **Then** the response contains id, email, display name, single role, and active flag.
2. **Given** no valid access credential, **When** profile is requested, **Then** the response is unauthorized.

---

### User Story 5 - Admin manages users (Priority: P3)

An Admin lists users, changes a user’s role among the allowed set (Initiator, Expert/Analyst, Decision Maker, Admin), and blocks or unblocks a user. A blocked user cannot sign in or renew sessions. Non-admins cannot use these operations.

**Why this priority**: Completes role model required by the assignment; depends on auth working first.

**Independent Test**: As Admin, list users, change role, block user; verify blocked user cannot sign in; as Initiator, admin APIs are forbidden.

**Acceptance Scenarios**:

1. **Given** an Admin access credential, **When** they list users, **Then** they see accounts with email, display name, role, and blocked flag.
2. **Given** an Admin, **When** they assign a user one of the four allowed roles, **Then** subsequent access decisions use the new role.
3. **Given** an Admin blocks a user, **When** that user tries to sign in or renew, **Then** the attempt fails as unauthorized (or equivalent denied), and existing refresh credentials for that user are revoked. An already-issued access credential MAY remain usable until its short TTL expires (accepted tradeoff for v1).
4. **Given** a non-Admin, **When** they call any admin user-management operation, **Then** access is forbidden.

---

### Edge Cases

- Refresh cookie missing, expired, reused after rotation, or already revoked → renewal fails unauthorized; no new access credential.
- Access credential malformed or expired on a protected operation → unauthorized (client may then try renewal).
- Blocked user with still-unexpired access credential → may call protected operations until access TTL ends; cannot renew or sign in; all refresh sessions are already revoked at block time (v1 tradeoff, not instant access kill).
- Admin attempts to block or demote the last remaining Admin → rejected to avoid lockout.
- Registration with invalid email format or password below minimum policy → validation error, no account created.
- Concurrent renewals with the same refresh credential → at most one succeeds; the other fails after rotation (no dual valid refresh chain from one token).
- User signed in on two browsers → both have independent refresh sessions; logout in one does not log out the other; block kills all refresh sessions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support accounts identified by unique email, with display name, password verifier (never store or return plaintext passwords), single role, and active/blocked status.
- **FR-002**: System MUST support exactly these roles: Initiator, Expert/Analyst, Decision Maker, Admin. Each user has exactly one role at a time.
- **FR-003**: System MUST allow public self-registration that always creates an Initiator account (never an elevated role).
- **FR-004**: System MUST provide a one-time bootstrap that creates the first Admin only when the user store is empty; afterward bootstrap MUST be disabled.
- **FR-005**: System MUST authenticate email+password sign-in and, on success, issue a short-lived access credential in the response body plus a long-lived refresh credential delivered exclusively via an HttpOnly cookie that page scripts cannot read (Secure and SameSite restrictions apply in deployed HTTPS environments). This hybrid model is the canonical session design for the feature (access is not stored in an HttpOnly cookie; refresh is never returned in a JSON body or readable storage).
- **FR-006**: System MUST accept the access credential on protected operations (bearer-style client header) and reject missing/invalid/expired access with unauthorized. Alternative models (session-cookie-only, both tokens in cookies, or storing refresh in frontend-readable storage) are explicitly out of scope.
- **FR-007**: System MUST expose a renewal operation that, given a valid non-revoked refresh cookie, issues a new access credential and rotates the refresh credential (invalidate the old one, set the new cookie).
- **FR-008**: System MUST expose sign-out that revokes only the current refresh credential server-side and clears that refresh cookie; other devices’ refresh sessions remain valid until they sign out, expire, or the user is blocked.
- **FR-009**: System MUST persist refresh credentials server-side in a form that allows revoke/lookup without storing the raw token in recoverable plaintext (store a one-way hash or equivalent). A user MAY have multiple active refresh sessions concurrently (one per sign-in/device).
- **FR-010**: System MUST expose a “current user profile” operation for the authenticated subject.
- **FR-011**: System MUST allow Admin to list users, change a user’s role within the allowed set, and block/unblock users; non-Admins MUST receive forbidden.
- **FR-012**: System MUST revoke all refresh credentials for a user when that user is blocked, and MUST deny sign-in and renewal for blocked users. Instant invalidation of already-issued access credentials is NOT required in v1; security window is bounded by the short access TTL (target ~10–15 minutes).
- **FR-013**: Failed sign-in MUST use a uniform unauthorized response that does not reveal whether the email is registered.
- **FR-014**: System MUST validate registration and admin mutation inputs (email format, password minimum length, required fields) and reject invalid input without partial writes.
- **FR-015**: Automated tests MUST cover sign-in, renewal, sign-out, role-forbidden admin access, and block behavior (unit and integration levels as required by project constitution).

### Key Entities

- **User**: Person account — email (unique), display name, password verifier, single role, blocked flag, timestamps.
- **Role**: One of Initiator, Expert/Analyst, Decision Maker, Admin — drives authorization decisions.
- **Refresh Session**: Server-side record tying a user to a refresh credential (hash), expiry, revoked flag; used for renewal and logout.
- **Access Credential**: Short-lived proof of authentication carried by the client for API calls (not persisted as a server session row in v1).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can register and complete first sign-in in under 2 minutes without administrator help.
- **SC-002**: After sign-out, 100% of renewal attempts with the old refresh credential fail in verification tests.
- **SC-003**: In verification tests, a non-Admin is denied on every admin user-management operation (forbidden), and an Admin succeeds on the same operations.
- **SC-004**: Wrong-password and unknown-email sign-in failures are indistinguishable to the client (same status and error shape) in 100% of tested cases.
- **SC-005**: Page-script access to the refresh credential is not possible by design (HttpOnly); this property is documented in the feature acceptance notes and verified in integration tests via Set-Cookie attributes.
- **SC-006**: Access renewal after access expiry succeeds for an active user with a valid refresh cookie without re-entering the password, in under 5 seconds under normal local test conditions.

## Assumptions

- Login identifier is email (not a separate username).
- One role per user (no multi-role matrix in this feature).
- Password policy minimum: at least 8 characters; no compulsory MFA in v1.
- Access credential lifetime on the order of 10–15 minutes; refresh on the order of 7–14 days (exact values chosen in planning).
- Refresh token format (opaque vs signed) is a planning decision; behavior above is mandatory either way.
- Session model clarification (2026-10-18): hybrid access-body + refresh-HttpOnly-cookie is locked; do not redesign to pure server sessions or dual-cookie access unless constitution/spec are amended.
- Access revocation on block (2026-10-18): v1 accepts TTL-bounded residual access; no ban-list cache and no mandatory DB blocked-check on every request for this feature. Stronger instant-kill strategies are future enhancements.
- Email verification, password reset, OAuth/SSO, and full UI are out of scope (future).
- Project registry, portfolio budget, criteria, calc-service, and finance-service are out of scope for this feature.
- Implementation will follow the core constitution (Maven `api`/`impl`, contract DTOs as records with validation in `api`, MapStruct in `impl`, test pyramid, no Keycloak).
- “Expert/Analyst” is one role label in the product (single enum value), not two separate roles.
