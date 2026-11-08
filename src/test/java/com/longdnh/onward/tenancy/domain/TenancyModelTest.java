package com.longdnh.donot_get_left_behind.tenancy.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TenancyModelTest {

    private static final UUID USER = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final UUID ORG_A = UUID.fromString("019a0000-0000-7000-8000-000000000002");
    private static final UUID ORG_B = UUID.fromString("019a0000-0000-7000-8000-000000000003");
    private static final UUID MEMBERSHIP = UUID.fromString("019a0000-0000-7000-8000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-20T00:00:00Z");

    @Test
    void enforcesNameLengthOnCreationAndRenameAfterStrippingWhitespace() {
        String name = "a".repeat(200);
        Organization organization = Organization.create(ORG_A, "  " + name + "  ", NOW);
        assertEquals(name, organization.getName());
        assertEquals(name, organization.rename(name, NOW.plusSeconds(1)).getName());
        assertThrows(IllegalArgumentException.class,
                () -> Organization.create(ORG_A, name + "a", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> organization.rename(name + "a", NOW.plusSeconds(1)));
    }

    @Test
    void countsUnicodeCodePointsInsteadOfUtf16CodeUnits() {
        String character = "\uD83D\uDE00";
        Organization organization = Organization.create(ORG_A, character.repeat(200), NOW);
        assertEquals(400, organization.getName().length());
        assertThrows(IllegalArgumentException.class,
                () -> Organization.create(ORG_A, character.repeat(201), NOW));
    }

    @Test
    void organizationRenamePreservesIdentityAndOriginalState() {
        Organization original = Organization.create(ORG_A, "  Research Team  ", NOW);
        Organization renamed = original.rename("New name", NOW.plusSeconds(1));

        assertEquals("Research Team", original.getName());
        assertEquals("New name", renamed.getName());
        assertEquals(original.getId(), renamed.getId());
        assertEquals(NOW, renamed.getCreatedAt());
        assertEquals(NOW.plusSeconds(1), renamed.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> renamed.rename(" ", NOW.plusSeconds(2)));
        assertThrows(IllegalArgumentException.class, () -> renamed.rename("Earlier", NOW));
    }

    @Test
    void oneUserCanHaveDifferentRolesInDifferentOrganizations() {
        Membership owner = Membership.create(MEMBERSHIP, ORG_A, USER, Role.OWNER, NOW);
        Membership member = Membership.create(UUID.fromString("019a0000-0000-7000-8000-000000000005"),
                ORG_B, USER, Role.MEMBER, NOW);

        assertEquals(owner.getUserId(), member.getUserId());
        assertNotEquals(owner.getOrganizationId(), member.getOrganizationId());
        assertEquals(Role.OWNER, owner.getRole());
        assertEquals(Role.MEMBER, member.getRole());
    }

    @Test
    void removalAndReactivationPreserveMembershipIdentityButUseTheNewRole() {
        Membership original = Membership.create(MEMBERSHIP, ORG_A, USER, Role.ADMIN, NOW);
        Membership removed = original.remove(NOW.plusSeconds(1));
        Membership reactivated = removed.reactivate(Role.MEMBER, NOW.plusSeconds(2));

        assertTrue(original.isActive());
        assertFalse(removed.isActive());
        assertEquals(Role.ADMIN, removed.getRole());
        assertSame(removed, removed.remove(NOW.plusSeconds(2)));
        assertTrue(reactivated.isActive());
        assertEquals(Role.MEMBER, reactivated.getRole());
        assertEquals(MEMBERSHIP, reactivated.getId());
        assertEquals(ORG_A, reactivated.getOrganizationId());
        assertEquals(USER, reactivated.getUserId());
        assertEquals(NOW, reactivated.getCreatedAt());
        assertEquals(NOW.plusSeconds(2), reactivated.getUpdatedAt());
    }

    @Test
    void transitionsRejectInvalidStatesAndBackwardTime() {
        Membership active = Membership.create(MEMBERSHIP, ORG_A, USER, Role.MEMBER, NOW);
        Membership changed = active.changeRole(Role.ADMIN, NOW.plusSeconds(2));
        Membership removed = changed.remove(NOW.plusSeconds(3));

        assertEquals(Role.MEMBER, active.getRole());
        assertEquals(Role.ADMIN, changed.getRole());
        assertThrows(IllegalStateException.class, () -> active.reactivate(Role.MEMBER, NOW));
        assertThrows(IllegalStateException.class, () -> removed.changeRole(Role.OWNER, NOW.plusSeconds(4)));
        assertThrows(IllegalArgumentException.class, () -> changed.changeRole(Role.MEMBER, NOW));
        assertThrows(IllegalArgumentException.class, () -> changed.remove(NOW));
        assertThrows(IllegalArgumentException.class, () -> removed.reactivate(Role.MEMBER, NOW));
        assertThrows(NullPointerException.class, () -> active.changeRole(null, NOW));
    }

    @Test
    void validatesUuidV7ReferencesAndRestoredTimestamps() {
        UUID v4 = UUID.fromString("019a0000-0000-4000-8000-000000000001");
        UUID wrongVariant = UUID.fromString("019a0000-0000-7000-0000-000000000001");
        assertThrows(IllegalArgumentException.class, () -> Organization.create(v4, "Team", NOW));
        assertThrows(IllegalArgumentException.class, () -> Organization.create(wrongVariant, "Team", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Membership.create(v4, ORG_A, USER, Role.MEMBER, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Membership.create(MEMBERSHIP, v4, USER, Role.MEMBER, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Membership.create(MEMBERSHIP, ORG_A, v4, Role.MEMBER, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Membership(MEMBERSHIP, ORG_A, USER, Role.MEMBER,
                        Membership.MembershipStatus.ACTIVE, NOW, NOW.minusSeconds(1)));
    }
}
