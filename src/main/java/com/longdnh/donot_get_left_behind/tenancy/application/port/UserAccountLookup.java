package com.longdnh.donot_get_left_behind.tenancy.application.port;

import java.util.UUID;

/**
 * What tenancy needs to know about user accounts, and nothing more. Tenancy owns this port;
 * the adapter in tenancy.infrastructure decides how to answer (today: the users table).
 */
public interface UserAccountLookup {

    /** True only for an existing account whose status allows it to be added to organizations. */
    boolean isActiveUser(UUID userId);
}
