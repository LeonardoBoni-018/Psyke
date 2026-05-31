package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request to create a new prescription")
public record CreatePrescriptionRequest(
    @NotNull @Schema(description = "Patient ID") UUID patientId,
    @NotNull @Schema(description = "Professional ID") UUID professionalId,
    @Schema(description = "Appointment ID (optional)") UUID appointmentId,
    @NotBlank @Size(max = 500) @Schema(description = "Medication name", example = "Sertralina 50mg") String medication,
    @Size(max = 200) @Schema(description = "Dosage", example = "1 comprimido ao dia") String dosage,
    @Schema(description = "Usage instructions") String instructions,
    @Schema(description = "Start date", example = "2026-06-01") LocalDate startDate,
    @Schema(description = "End date", example = "2026-09-01") LocalDate endDate
) {}
