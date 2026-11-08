-- System-wide role, distinct from tenant roles on memberships. NULL means an ordinary user.
-- The first admin is granted at startup from app.bootstrap.system-admin-email; later admins
-- are promoted through the admin API. Nothing is seeded here on purpose: no password hashes in migrations.
ALTER TABLE users ADD COLUMN system_role varchar(32);
ALTER TABLE users ADD CONSTRAINT ck_users_system_role CHECK (system_role IS NULL OR system_role IN ('ADMIN'));
