package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Request to update an existing prescription")
public record UpdatePrescriptionRequest(
    @Size(max = 500) @Schema(description = "Medication name", example = "Sertralina 50mg") String medication,
    @Size(max = 200) @Schema(description = "Dosage", example = "1 comprimido ao dia") String dosage,
    @Schema(description = "Usage instructions") String instructions,
    @Schema(description = "Start date", example = "2026-06-01") LocalDate startDate,
    @Schema(description = "End date", example = "2026-09-01") LocalDate endDate,
    @Schema(description = "Whether the prescription is active", example = "true") Boolean active
) {}
