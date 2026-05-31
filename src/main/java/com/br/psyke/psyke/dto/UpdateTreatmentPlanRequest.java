package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Request to update an existing treatment plan")
public record UpdateTreatmentPlanRequest(
    @Size(max = 200) @Schema(description = "Plan title", example = "Plano para Ansiedade") String title,
    @Schema(description = "Plan description") String description,
    @Schema(description = "Therapeutic goals") String goals,
    @Schema(description = "Techniques to be used") String techniques,
    @Size(max = 100) @Schema(description = "Session frequency", example = "weekly") String frequency,
    @Positive @Schema(description = "Estimated number of sessions", example = "20") Integer estimatedSessions,
    @Size(max = 30) @Schema(description = "Status: ACTIVE, COMPLETED, SUSPENDED, CANCELLED", example = "ACTIVE") String status,
    @Schema(description = "Start date", example = "2026-06-01") LocalDate startDate,
    @Schema(description = "End date", example = "2026-12-01") LocalDate endDate
) {}
