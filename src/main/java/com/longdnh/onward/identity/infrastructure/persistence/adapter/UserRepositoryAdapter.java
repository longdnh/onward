package com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.adapter;

import static com.longdnh.donot_get_left_behind.common.Validation.requireNonBlank;
import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.identity.application.exception.EmailAlreadyExistsException;
import com.longdnh.donot_get_left_behind.identity.application.port.UserRepository;
import com.longdnh.donot_get_left_behind.identity.domain.SystemRole;
import com.longdnh.donot_get_left_behind.identity.domain.User;
import com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.entities.UserEntity;
import com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.repository.SpringDataUserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;

@Repository
@Transactional(readOnly = true)
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final EntityManager entityManager;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository, EntityManager entityManager) {
        this.springDataUserRepository = springDataUserRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(requireUuidV7(id, "id"))
                .map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String canonicalEmail) {
        return springDataUserRepository.findByEmail(requireNonBlank(canonicalEmail, "email"))
                .map(UserEntity::toDomain);
    }

    @Override
    @Transactional
    public User create(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        try {
            entityManager.persist(entity);
            // Check the non-deferrable unique constraint here, including concurrent registrations.
            entityManager.flush();
            return entity.toDomain();
        } catch (PersistenceException exception) {
            if (isDuplicateEmail(exception)) {
                throw new EmailAlreadyExistsException();
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        UserEntity savedEntity = springDataUserRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<User> findByIdForUpdate(UUID id) {
        return Optional.ofNullable(
                        entityManager.find(UserEntity.class, requireUuidV7(id, "id"), LockModeType.PESSIMISTIC_WRITE))
                .map(UserEntity::toDomain);
    }

    @Override
    public boolean existsSystemAdmin() {
        Long count = entityManager.createQuery(
                        "select count(u) from UserEntity u where u.systemRole = :role", Long.class)
                .setParameter("role", SystemRole.ADMIN)
                .getSingleResult();
        return count > 0;
    }

    private static boolean isDuplicateEmail(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && "uq_users_email".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
