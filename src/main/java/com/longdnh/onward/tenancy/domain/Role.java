package com.longdnh.onward.tenancy.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Role inside one organization. The permission sets are the matrix in
 * docs/authentication-authorization.md §5; they say nothing about target-specific rules
 * (the owner cannot be removed or demoted; admin cannot touch an admin), which application
 * services check separately.
 */
public enum Role {
    OWNER(EnumSet.allOf(Permission.class)),
    ADMIN(EnumSet.of(
            Permission.READ_ORGANIZATION,
            Permission.UPDATE_ORGANIZATION,
            Permission.READ_OWN_MEMBERSHIP,
            Permission.LIST_MEMBERSHIPS,
            Permission.ADD_MEMBER,
            Permission.REMOVE_MEMBER)),
    MEMBER(EnumSet.of(
            Permission.READ_ORGANIZATION,
            Permission.READ_OWN_MEMBERSHIP));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public boolean allows(Permission permission) {
        return permissions.contains(permission);
    }
}
