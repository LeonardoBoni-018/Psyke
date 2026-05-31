package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Questionnaire template response")
public record QuestionnaireResponse(
    String id,
    UUID clinicId,
    String title,
    String description,
    String questions,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
