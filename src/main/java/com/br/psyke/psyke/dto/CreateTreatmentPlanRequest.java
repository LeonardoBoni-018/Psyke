package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request to create a new treatment plan")
public record CreateTreatmentPlanRequest(
    @NotNull @Schema(description = "Prontuario ID") UUID prontuarioId,
    @NotBlank @Size(max = 200) @Schema(description = "Plan title", example = "Plano para Ansiedade") String title,
    @Schema(description = "Plan description") String description,
    @Schema(description = "Therapeutic goals") String goals,
    @Schema(description = "Techniques to be used") String techniques,
    @Size(max = 100) @Schema(description = "Session frequency", example = "weekly") String frequency,
    @Positive @Schema(description = "Estimated number of sessions", example = "20") Integer estimatedSessions,
    @Schema(description = "Start date", example = "2026-06-01") LocalDate startDate,
    @Schema(description = "End date", example = "2026-12-01") LocalDate endDate
) {}
