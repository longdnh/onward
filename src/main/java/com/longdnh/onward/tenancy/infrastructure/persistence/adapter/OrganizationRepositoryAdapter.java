package com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.common.pagination.PageResult;
import com.longdnh.donot_get_left_behind.tenancy.application.dto.OrganizationSummary;
import com.longdnh.donot_get_left_behind.tenancy.application.port.OrganizationRepository;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.domain.Role;
import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;
import com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.entities.OrganizationEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Tuple;

@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final EntityManager entityManager;

    public OrganizationRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Organization create(Organization organization) {
        OrganizationEntity entity = OrganizationEntity.fromDomain(organization);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Organization> findByIdForUpdate(UUID organizationId) {
        return Optional.ofNullable(
                        entityManager.find(OrganizationEntity.class, organizationId, LockModeType.PESSIMISTIC_WRITE))
                .map(OrganizationEntity::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Organization update(Organization organization) {
        // The domain object is immutable, so the entity is rebuilt and merged onto the managed row.
        // created_at is updatable=false on the entity, so only name and updated_at reach the UPDATE.
        return entityManager.merge(OrganizationEntity.fromDomain(organization)).toDomain();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Optional<Organization> findById(UUID organizationId) {
        return Optional.ofNullable(entityManager.find(OrganizationEntity.class, organizationId))
                .map(OrganizationEntity::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public PageResult<OrganizationSummary> findActiveByMemberUserId(UUID userId, PageRequest page) {
        // Entities carry plain ids instead of JPA associations, so the join is written explicitly.
        // Filter on (user_id, status) matches idx_memberships_user_status from V1.
        List<OrganizationSummary> items = entityManager.createQuery(
                        "select o as organization, m.role as role "
                                + "from MembershipEntity m, OrganizationEntity o "
                                + "where o.id = m.organizationId and m.userId = :userId and m.status = :status "
                                + "order by o.id",
                        Tuple.class)
                .setParameter("userId", userId)
                .setParameter("status", Membership.MembershipStatus.ACTIVE)
                .setFirstResult(page.offset())
                .setMaxResults(page.size())
                .getResultList()
                .stream()
                .map(row -> new OrganizationSummary(
                        row.get("organization", OrganizationEntity.class).toDomain(),
                        row.get("role", Role.class)))
                .toList();

        long total = entityManager.createQuery(
                        "select count(m) from MembershipEntity m where m.userId = :userId and m.status = :status",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("status", Membership.MembershipStatus.ACTIVE)
                .getSingleResult();

        return PageResult.of(items, page, total);
    }
}
