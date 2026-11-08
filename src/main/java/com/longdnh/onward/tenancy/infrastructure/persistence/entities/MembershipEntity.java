package com.longdnh.onward.tenancy.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.longdnh.onward.tenancy.domain.Membership;
import com.longdnh.onward.tenancy.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(name = "memberships", uniqueConstraints = @UniqueConstraint(name = "uq_memberships_organization_user", columnNames = {"organization_id", "user_id"}))
public class MembershipEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Membership.MembershipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MembershipEntity() {
    }

    public static MembershipEntity fromDomain(Membership membership) {
        Objects.requireNonNull(membership, "membership is required");
        MembershipEntity entity = new MembershipEntity();
        entity.id = membership.getId();
        entity.organizationId = membership.getOrganizationId();
        entity.userId = membership.getUserId();
        entity.role = membership.getRole();
        entity.status = membership.getStatus();
        entity.createdAt = membership.getCreatedAt();
        entity.updatedAt = membership.getUpdatedAt();
        return entity;
    }

    public Membership toDomain() {
        return new Membership(id, organizationId, userId, role, status, createdAt, updatedAt);
    }
}
