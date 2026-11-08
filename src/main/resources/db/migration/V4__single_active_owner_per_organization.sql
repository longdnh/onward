-- Single-owner model: each organization has exactly one ACTIVE OWNER membership.
-- "At most one" is enforced here by the database; "at least one" is kept by the application
-- (organization creation always inserts the owner, and the owner's membership can only change
-- through ownership transfer, which swaps roles inside one transaction).
DROP INDEX IF EXISTS idx_memberships_active_owners;

CREATE UNIQUE INDEX uq_memberships_single_active_owner ON memberships (organization_id)
    WHERE role = 'OWNER' AND status = 'ACTIVE';
