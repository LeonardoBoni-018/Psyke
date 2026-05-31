package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Request to create a new invoice")
public record CreateInvoiceRequest(
    @Schema(description = "Appointment ID (optional)") UUID appointmentId,
    @NotNull @Schema(description = "Patient ID") UUID patientId,
    @NotNull @Schema(description = "Professional ID") UUID professionalId,
    @NotNull @Positive @Schema(description = "Amount", example = "250.00") BigDecimal amount,
    @Size(max = 50) @Schema(description = "Payment method", example = "credit_card") String paymentMethod,
    @Schema(description = "Payment date") OffsetDateTime paymentDate,
    @Schema(description = "Invoice notes") String notes
) {}
