package com.longdnh.onward.tenancy.domain;

/** One row of the action table in docs/authentication-authorization.md §5 (single-owner model). */
public enum Permission {
    READ_ORGANIZATION,
    UPDATE_ORGANIZATION,
    READ_OWN_MEMBERSHIP,
    LIST_MEMBERSHIPS,
    ADD_MEMBER,
    ADD_ADMIN,
    CHANGE_ROLE,
    REMOVE_MEMBER,
    REMOVE_ADMIN,
    TRANSFER_OWNERSHIP
}
