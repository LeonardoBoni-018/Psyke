package com.br.psyke.psyke.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request for AI-suggested appointment slots")
public record SlotSuggestionRequest(
    @NotNull @Schema(description = "Professional ID") UUID professionalId,
    @Schema(description = "Preferred days of week (1=Mon..7=Sun)", example = "[1, 3, 5]") java.util.List<Integer> preferredDays,
    @Schema(description = "Preferred time range start", example = "08:00") String preferredStart,
    @Schema(description = "Preferred time range end", example = "18:00") String preferredEnd,
    @Schema(description = "Session duration in minutes", example = "50") int durationMinutes,
    @Schema(description = "Start date for suggestions", example = "2026-06-01") LocalDate from
) {}
