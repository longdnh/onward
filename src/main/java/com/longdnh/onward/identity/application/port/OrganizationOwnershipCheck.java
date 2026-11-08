package com.longdnh.onward.identity.application.port;

import java.util.List;
import java.util.UUID;

/**
 * What identity needs to know from tenancy before disabling an account. Identity owns this
 * port; the adapter reads the memberships table. Only valid while the caller holds the user row lock,
 * because add-member and ownership transfer lock the target user (FOR SHARE) before writing.
 */
public interface OrganizationOwnershipCheck {

    List<UUID> organizationsOwnedBy(UUID userId);
}
