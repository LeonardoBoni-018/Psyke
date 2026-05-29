package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Request to add recurrence to an existing appointment")
public record AppointmentRecurrenceRequest(
    @NotNull @Schema(description = "Frequency (WEEKLY, BIWEEKLY, MONTHLY)", example = "WEEKLY") String freq,
    @Schema(description = "Interval between occurrences (default 1)", example = "1") Integer interval,
    @Schema(description = "Number of occurrences (optional if until is set)", example = "12") Integer count,
    @Schema(description = "End date (optional if count is set)", example = "2026-12-31") LocalDate until
) {}
