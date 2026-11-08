package com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.entities.UserEntity;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
}
