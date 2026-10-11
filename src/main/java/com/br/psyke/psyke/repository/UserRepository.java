package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code users} lives in the shared master schema: always scope queries by tenant. */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    Optional<User> findByEmailIgnoreCaseAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailIgnoreCaseAndTenantId(String email, UUID tenantId);
    List<User> findByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = "roles")
    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);
}
