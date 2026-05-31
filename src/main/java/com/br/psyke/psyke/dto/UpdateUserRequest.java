package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update an existing user")
public record UpdateUserRequest(
    @Size(max = 300) @Schema(description = "Full name", example = "Maria Silva") String fullName,
    @Size(max = 14) @Schema(description = "CPF", example = "12345678900") String cpf,
    @Size(max = 20) @Schema(description = "Phone", example = "+5511988887777") String phone,
    @Schema(description = "Whether the user is active", example = "true") Boolean active
) {}
