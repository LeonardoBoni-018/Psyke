package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Request to update an existing appointment")
public record UpdateAppointmentRequest(
    @Schema(description = "New room ID (optional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa8") UUID roomId,
    @Schema(description = "Appointment notes", example = "Updated notes for this session") String notes
) {}
