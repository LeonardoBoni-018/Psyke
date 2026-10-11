package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to initiate password reset")
public record ForgotPasswordRequest(
    @NotBlank @Email @Schema(description = "User email", example = "user@example.com") String email,
    @NotBlank @Size(max = 64) @Schema(description = "Clinic identifier: tenant UUID or slug", example = "clinica-serenity") String tenantId
) {}
