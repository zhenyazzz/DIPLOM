-- Seed admin: email admin@portfolio.local / password Admin123!
INSERT INTO identity.users (id, email, display_name, password_hash, role, blocked, created_at, updated_at)
VALUES (
  '00000000-0000-4000-8000-000000000001',
  'admin@portfolio.local',
  'System Admin',
  '$2b$10$aZ8G3hexJX.3Fy87FYEUIetIZQ/NpF0N0Vuj9.zxIH6HtzeMviqiO',
  'ADMIN', FALSE, NOW(), NOW()
)
ON CONFLICT (email) DO NOTHING;
