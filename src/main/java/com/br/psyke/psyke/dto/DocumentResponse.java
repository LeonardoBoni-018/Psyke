package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Document response")
public record DocumentResponse(
    String id,
    UUID clinicId,
    UUID patientId,
    UUID appointmentId,
    UUID professionalId,
    String originalName,
    String fileType,
    long fileSize,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
