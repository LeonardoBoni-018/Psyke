package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to initiate password reset")
public record ForgotPasswordRequest(
    @NotBlank @Email @Schema(description = "User email", example = "user@example.com") String email,
    @NotBlank @Schema(description = "Tenant ID", example = "550e8400-e29b-41d4-a716-446655440000") String tenantId
) {}
