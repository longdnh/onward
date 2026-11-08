package com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.tenancy.application.port.UserAccountLookup;

import jakarta.persistence.EntityManager;

/**
 * Reads identity's users table directly. Acceptable in a single-database monolith; if the
 * modules are ever split, only this class changes (to an HTTP call or a published view).
 */
@Repository
public class UserAccountLookupAdapter implements UserAccountLookup {

    private final EntityManager entityManager;

    public UserAccountLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public boolean isActiveUser(UUID userId) {
        Number count = (Number) entityManager.createNativeQuery(
                        "select count(*) from users where id = :id and status = 'ACTIVE'")
                .setParameter("id", userId)
                .getSingleResult();
        return count.longValue() > 0;
    }
}
