package com.br.psyke.psyke.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
    @NotBlank String name,
    @NotBlank String slug,
    @NotBlank String plan,
    @NotBlank @Email String adminEmail,
    @NotBlank String adminPassword,
    @NotBlank String adminName,
    Integer maxClinics,
    Integer maxUsers
) {}
