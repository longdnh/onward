package com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "organizations")
public class OrganizationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = Organization.MAX_NAME_LENGTH)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationEntity() {
    }

    public static OrganizationEntity fromDomain(Organization organization) {
        Objects.requireNonNull(organization, "organization is required");
        OrganizationEntity entity = new OrganizationEntity();
        entity.id = organization.getId();
        entity.name = organization.getName();
        entity.createdAt = organization.getCreatedAt();
        entity.updatedAt = organization.getUpdatedAt();
        return entity;
    }

    public Organization toDomain() {
        return new Organization(id, name, createdAt, updatedAt);
    }
}
