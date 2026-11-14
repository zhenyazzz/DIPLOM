ALTER TABLE files ADD COLUMN owner_id UUID;

UPDATE files SET owner_id = created_by::uuid;

ALTER TABLE files ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX ix_files_owner_id ON files (owner_id);
