package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Consent term response")
public record ConsentTermResponse(
    String id,
    UUID clinicId,
    UUID patientId,
    UUID professionalId,
    String title,
    String content,
    boolean signedByPatient,
    boolean signedByProfessional,
    OffsetDateTime signedAt,
    OffsetDateTime expiresAt,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
