package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Prescription response")
public record PrescriptionResponse(
    String id,
    UUID clinicId,
    UUID patientId,
    UUID professionalId,
    UUID appointmentId,
    String medication,
    String dosage,
    String instructions,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
