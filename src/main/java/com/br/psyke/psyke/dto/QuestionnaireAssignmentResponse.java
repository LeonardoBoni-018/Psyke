package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Questionnaire assignment response")
public record QuestionnaireAssignmentResponse(
    String id,
    UUID clinicId,
    UUID questionnaireId,
    UUID patientId,
    UUID professionalId,
    String status,
    String answers,
    BigDecimal score,
    OffsetDateTime assignedAt,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
