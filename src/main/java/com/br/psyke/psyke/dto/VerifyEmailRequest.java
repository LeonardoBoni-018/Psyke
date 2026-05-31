package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to verify email with token")
public record VerifyEmailRequest(
    @NotBlank @Schema(description = "Verification token") String token
) {}
