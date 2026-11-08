package com.longdnh.onward.tenancy.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.onward.tenancy.application.port.UserAccountLookup;

import java.util.List;

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

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockActiveUser(UUID userId) {
        List<String> status = entityManager.createNativeQuery(
                        "select status from users where id = :id for share", String.class)
                .setParameter("id", userId)
                .getResultList();
        return !status.isEmpty() && "ACTIVE".equals(status.get(0));
    }
}
