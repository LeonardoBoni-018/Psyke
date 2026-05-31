package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Request to create a consent term")
public record CreateConsentTermRequest(
    @NotNull @Schema(description = "Patient ID") UUID patientId,
    @NotNull @Schema(description = "Professional ID") UUID professionalId,
    @NotBlank @Size(max = 300) @Schema(description = "Term title", example = "Termo de Consentimento para Atendimento") String title,
    @NotBlank @Schema(description = "Full term content") String content,
    @Schema(description = "Expiration date") OffsetDateTime expiresAt
) {}
