package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Appointment resource")
public record AppointmentResponse(
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID clinicId,
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID patientId,
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID professionalId,
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID roomId,
    @Schema(example = "2026-05-28T09:00:00") String startTime,
    @Schema(example = "2026-05-28T09:50:00") String endTime,
    @Schema(example = "SCHEDULED") String status,
    @Schema(example = "Patient reported anxiety") String prontuario,
    @Schema(example = "Initial consultation") String notes,
    @Schema(example = "2026-05-28T08:00:00") String createdAt,
    @Schema(example = "2026-05-28T08:00:00") String updatedAt
) {}
