package com.longdnh.onward.identity.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.longdnh.onward.identity.domain.SystemRole;
import com.longdnh.onward.identity.domain.User;
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
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"))
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = User.MAX_EMAIL_LENGTH)
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private User.UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", length = 32)
    private SystemRole systemRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public static UserEntity fromDomain(User user) {
        Objects.requireNonNull(user, "user is required");
        UserEntity entity = new UserEntity();
        entity.id = user.getId();
        entity.email = user.getEmail();
        entity.passwordHash = user.getPasswordHash();
        entity.status = user.getStatus();
        entity.systemRole = user.getSystemRole();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    public User toDomain() {
        return new User(id, email, passwordHash, status, systemRole, createdAt, updatedAt);
    }
}
