package com.longdnh.onward;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.longdnh.onward.identity.domain.User;
import com.longdnh.onward.identity.infrastructure.persistence.entities.UserEntity;
import com.longdnh.onward.tenancy.domain.Membership;
import com.longdnh.onward.tenancy.domain.Organization;
import com.longdnh.onward.tenancy.domain.Role;
import com.longdnh.onward.tenancy.infrastructure.persistence.entities.MembershipEntity;
import com.longdnh.onward.tenancy.infrastructure.persistence.entities.OrganizationEntity;

class ModelMappingTest {

    private static final UUID USER = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final UUID ORG = UUID.fromString("019a0000-0000-7000-8000-000000000002");
    private static final UUID MEMBERSHIP = UUID.fromString("019a0000-0000-7000-8000-000000000003");
    private static final Instant CREATED = Instant.parse("2026-10-20T00:00:00Z");
    private static final Instant UPDATED = CREATED.plusSeconds(10);

    @ParameterizedTest
    @EnumSource(User.UserStatus.class)
    void userMappingPreservesCredentialsStatusAndHistory(User.UserStatus status) {
        User original = new User(USER, "alice+research@example.test", "encoded-hash", status, null, CREATED, CREATED, UPDATED);
        User restored = UserEntity.fromDomain(original).toDomain();

        assertAll(
                () -> assertEquals(USER, restored.getId()),
                () -> assertEquals(original.getEmail(), restored.getEmail()),
                () -> assertEquals(original.getPasswordHash(), restored.getPasswordHash()),
                () -> assertEquals(status, restored.getStatus()),
                () -> assertEquals(CREATED, restored.getCreatedAt()),
                () -> assertEquals(UPDATED, restored.getUpdatedAt()));
    }

    @Test
    void organizationMappingPreservesNameAndHistory() {
        Organization restored = OrganizationEntity.fromDomain(
                new Organization(ORG, "Research", CREATED, UPDATED)).toDomain();

        assertAll(
                () -> assertEquals(ORG, restored.getId()),
                () -> assertEquals("Research", restored.getName()),
                () -> assertEquals(CREATED, restored.getCreatedAt()),
                () -> assertEquals(UPDATED, restored.getUpdatedAt()));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void membershipMappingPreservesTenantRoleStatusAndHistory(Role role) {
        for (Membership.MembershipStatus status : Membership.MembershipStatus.values()) {
            Membership restored = MembershipEntity.fromDomain(new Membership(
                    MEMBERSHIP, ORG, USER, role, status, CREATED, UPDATED)).toDomain();

            assertAll(
                    () -> assertEquals(MEMBERSHIP, restored.getId()),
                    () -> assertEquals(ORG, restored.getOrganizationId()),
                    () -> assertEquals(USER, restored.getUserId()),
                    () -> assertEquals(role, restored.getRole()),
                    () -> assertEquals(status, restored.getStatus()),
                    () -> assertEquals(CREATED, restored.getCreatedAt()),
                    () -> assertEquals(UPDATED, restored.getUpdatedAt()));
        }
    }
}
