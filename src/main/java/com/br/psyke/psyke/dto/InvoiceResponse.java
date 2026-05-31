package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Invoice response")
public record InvoiceResponse(
    String id,
    UUID clinicId,
    UUID appointmentId,
    UUID patientId,
    UUID professionalId,
    BigDecimal amount,
    String paymentMethod,
    OffsetDateTime paymentDate,
    String status,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
