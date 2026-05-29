package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Request to create a new user within the authenticated tenant")
public record CreateUserRequest(
    @NotBlank @Schema(description = "Full name", example = "Maria Silva") String fullName,
    @NotBlank @Email @Schema(description = "Email address (unique per tenant)", example = "maria@example.com") String email,
    @NotBlank @Size(min = 8) @Schema(description = "Password (minimum 8 characters)") String password,
    @Schema(description = "CPF (optional)", example = "12345678900") String cpf,
    @Schema(description = "Phone number (optional)", example = "+5511988887777") String phone,
    @Schema(description = "Initial role names (optional, defaults to ROLE_PATIENT)", example = "[\"ROLE_PATIENT\"]") List<String> roles
) {}
