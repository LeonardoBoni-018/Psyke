package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Treatment plan response")
public record TreatmentPlanResponse(
    String id,
    UUID clinicId,
    UUID prontuarioId,
    String title,
    String description,
    String goals,
    String techniques,
    String frequency,
    Integer estimatedSessions,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
