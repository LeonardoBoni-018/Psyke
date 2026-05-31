package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.AuditLogResponse;
import com.br.psyke.psyke.model.AuditLog;
import com.br.psyke.psyke.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository logs;

    public Page<AuditLogResponse> findAll(UUID clinicId, String entityType, UUID entityId, Pageable pageable) {
        if (entityType != null && entityId != null)
            return logs.findByClinicIdAndEntityTypeAndEntityId(clinicId, entityType, entityId, pageable).map(this::toResponse);
        if (entityType != null)
            return logs.findByClinicIdAndEntityType(clinicId, entityType, pageable).map(this::toResponse);
        return logs.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    @Transactional
    public AuditLog log(UUID clinicId, String entityType, UUID entityId, String action, UUID userId, String details) {
        var log = AuditLog.builder()
                .clinicId(clinicId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .userId(userId)
                .details(details)
                .build();
        return logs.save(log);
    }

    private AuditLogResponse toResponse(AuditLog l) {
        return new AuditLogResponse(
                l.getId().toString(), l.getClinicId(),
                l.getEntityType(), l.getEntityId(),
                l.getAction(), l.getUserId(),
                l.getDetails(), l.getCreatedAt());
    }
}
