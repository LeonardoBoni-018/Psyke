package com.br.psyke.psyke.dto;

import com.br.psyke.psyke.model.Appointment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to transition an appointment status")
public record StatusUpdateRequest(
    @NotNull @Schema(description = "Target status", example = "CONFIRMED", allowableValues = {"CONFIRMED", "CANCELLED", "NO_SHOW", "DONE"})
    Appointment.AppointmentStatus status,
    @Schema(description = "Reason for cancellation or status change", example = "Patient requested cancellation")
    String reason
) {}
