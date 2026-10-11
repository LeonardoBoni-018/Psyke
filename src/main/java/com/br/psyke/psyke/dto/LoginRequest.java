package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 128) String password,
    @NotBlank @Size(max = 64)
    @Schema(description = "Clinic identifier: tenant UUID or slug", example = "clinica-serenity") String tenantId
) {}
