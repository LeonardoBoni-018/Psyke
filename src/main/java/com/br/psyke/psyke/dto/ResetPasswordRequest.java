package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to reset password with token")
public record ResetPasswordRequest(
    @NotBlank @Schema(description = "Reset token received by email") String token,
    @NotBlank @Size(min = 8) @Schema(description = "New password (minimum 8 characters)") String newPassword
) {}
