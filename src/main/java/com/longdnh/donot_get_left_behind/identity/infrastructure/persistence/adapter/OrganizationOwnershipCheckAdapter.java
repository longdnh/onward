package com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.identity.application.port.OrganizationOwnershipCheck;

import jakarta.persistence.EntityManager;

/** Reads tenancy's memberships table directly; the mirror image of tenancy's UserAccountLookupAdapter. */
@Repository
public class OrganizationOwnershipCheckAdapter implements OrganizationOwnershipCheck {

    private final EntityManager entityManager;

    public OrganizationOwnershipCheckAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<UUID> organizationsOwnedBy(UUID userId) {
        return entityManager.createNativeQuery(
                        "select organization_id from memberships where user_id = :userId and role = 'OWNER' and status = 'ACTIVE'",
                        UUID.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
