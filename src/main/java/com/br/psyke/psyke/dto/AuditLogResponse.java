package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Audit log entry")
public record AuditLogResponse(
    String id,
    UUID clinicId,
    String entityType,
    UUID entityId,
    String action,
    UUID userId,
    String details,
    OffsetDateTime createdAt
) {}
