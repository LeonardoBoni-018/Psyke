package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByClinicId(UUID clinicId, Pageable pageable);
    Page<AuditLog> findByClinicIdAndEntityType(UUID clinicId, String entityType, Pageable pageable);
    Page<AuditLog> findByClinicIdAndEntityTypeAndEntityId(UUID clinicId, String entityType, UUID entityId, Pageable pageable);
}
