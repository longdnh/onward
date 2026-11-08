package com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.common.pagination.PageResult;
import com.longdnh.donot_get_left_behind.tenancy.application.port.MembershipRepository;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.entities.MembershipEntity;

import jakarta.persistence.EntityManager;

@Repository
public class MembershipRepositoryAdapter implements MembershipRepository {

    private final EntityManager entityManager;

    public MembershipRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Membership create(Membership membership) {
        MembershipEntity entity = MembershipEntity.fromDomain(membership);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Optional<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId) {
        // Both ids are always in the predicate: a membership is only ever addressed within its tenant.
        return entityManager.createQuery(
                        "select m from MembershipEntity m where m.organizationId = :organizationId and m.userId = :userId",
                        MembershipEntity.class)
                .setParameter("organizationId", organizationId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(MembershipEntity::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Membership update(Membership membership) {
        return entityManager.merge(MembershipEntity.fromDomain(membership)).toDomain();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public PageResult<Membership> findByOrganizationId(UUID organizationId, PageRequest page) {
        List<Membership> items = entityManager.createQuery(
                        "select m from MembershipEntity m where m.organizationId = :organizationId order by m.id",
                        MembershipEntity.class)
                .setParameter("organizationId", organizationId)
                .setFirstResult(page.offset())
                .setMaxResults(page.size())
                .getResultList()
                .stream()
                .map(MembershipEntity::toDomain)
                .toList();

        long total = entityManager.createQuery(
                        "select count(m) from MembershipEntity m where m.organizationId = :organizationId",
                        Long.class)
                .setParameter("organizationId", organizationId)
                .getSingleResult();

        return PageResult.of(items, page, total);
    }
}
