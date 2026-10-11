package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public clinic signup. {@code maxClinics}/{@code maxUsers} are accepted for backwards compatibility
 * but ignored: plan limits are decided by the server, never by the client.
 */
public record CreateTenantRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank
    @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{1,38}[a-z0-9])$",
             message = "slug must be 3-40 lowercase letters, digits or hyphens")
    @Schema(example = "clinica-serenity") String slug,
    @NotBlank
    @Pattern(regexp = "(?i)^(BASIC|PRO|ENTERPRISE)$", message = "plan must be BASIC, PRO or ENTERPRISE")
    String plan,
    @NotBlank @Email @Size(max = 255) String adminEmail,
    @NotBlank @Size(min = 8, max = 128, message = "adminPassword must have 8 to 128 characters") String adminPassword,
    @NotBlank @Size(max = 300) String adminName,
    Integer maxClinics,
    Integer maxUsers
) {}
