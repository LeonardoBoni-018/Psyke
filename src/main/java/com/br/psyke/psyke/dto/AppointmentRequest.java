package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Request to schedule a new appointment")
public record AppointmentRequest(
    @NotNull @Schema(description = "Patient ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID patientId,
    @NotNull @Schema(description = "Professional ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa7") UUID professionalId,
    @Schema(description = "Room ID (optional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa8") UUID roomId,
    @NotNull @Schema(description = "Start time", example = "2026-05-28T09:00:00") LocalDateTime startTime,
    @NotNull @Schema(description = "End time", example = "2026-05-28T09:50:00") LocalDateTime endTime,
    @Schema(description = "Appointment notes", example = "Initial consultation - anxiety assessment") String notes
) {}
