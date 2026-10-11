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
    OffsetDateTime updatedAt,
    @Schema(description = "Patient display name") String patientName,
    @Schema(description = "Professional display name") String professionalName
) {
    public InvoiceResponse(String id, UUID clinicId, UUID appointmentId, UUID patientId, UUID professionalId,
                           BigDecimal amount, String paymentMethod, OffsetDateTime paymentDate, String status,
                           String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, clinicId, appointmentId, patientId, professionalId, amount, paymentMethod, paymentDate, status,
             notes, createdAt, updatedAt, null, null);
    }
}
