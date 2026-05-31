package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Request to update an existing invoice")
public record UpdateInvoiceRequest(
    @Positive @Schema(description = "Amount", example = "300.00") BigDecimal amount,
    @Size(max = 50) @Schema(description = "Payment method", example = "pix") String paymentMethod,
    @Schema(description = "Payment date") OffsetDateTime paymentDate,
    @Size(max = 30) @Schema(description = "Status: PENDING, PAID, CANCELLED, REFUNDED", example = "PAID") String status,
    @Schema(description = "Invoice notes") String notes
) {}
