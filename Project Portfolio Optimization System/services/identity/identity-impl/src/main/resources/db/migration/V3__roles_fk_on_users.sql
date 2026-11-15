-- Normalize roles to a reference table (1:N): roles ← users.role_id.
-- Migrates from users.role (VARCHAR), then drops that column.

CREATE TABLE identity.roles (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code CHECK (code IN (
        'INITIATOR', 'EXPERT_ANALYST', 'DECISION_MAKER', 'ADMIN'
    ))
);

INSERT INTO identity.roles (id, code, name, description, created_at) VALUES
  ('00000000-0000-4000-8000-000000000011', 'INITIATOR',
   'Initiator', 'Project initiator / project manager', NOW()),
  ('00000000-0000-4000-8000-000000000012', 'EXPERT_ANALYST',
   'Expert analyst', 'Expert, financial analyst, portfolio manager', NOW()),
  ('00000000-0000-4000-8000-000000000013', 'DECISION_MAKER',
   'Decision maker', 'Committee member / approver', NOW()),
  ('00000000-0000-4000-8000-000000000014', 'ADMIN',
   'Administrator', 'System administrator', NOW())
ON CONFLICT (code) DO NOTHING;

ALTER TABLE identity.users ADD COLUMN role_id UUID;

UPDATE identity.users u
SET role_id = r.id
FROM identity.roles r
WHERE r.code = u.role;

ALTER TABLE identity.users
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE identity.users
    ADD CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES identity.roles (id) ON DELETE RESTRICT;

CREATE INDEX idx_users_role_id ON identity.users (role_id);

ALTER TABLE identity.users DROP COLUMN role;
