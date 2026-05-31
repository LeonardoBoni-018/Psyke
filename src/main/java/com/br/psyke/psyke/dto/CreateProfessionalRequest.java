package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request to create a new professional")
public record CreateProfessionalRequest(
    @NotBlank @Size(max = 20) @Schema(description = "CRP registration", example = "06/12345") String crp,
    @NotBlank @Size(max = 300) @Schema(description = "Full name", example = "Dra. Ana Beatriz") String fullName,
    @Size(max = 200) @Schema(description = "Specialty", example = "Psicologia Clínica") String specialty,
    @Size(max = 200) @Schema(description = "Therapeutic approach", example = "TCC") String approach,
    @Schema(description = "Professional resume") String resume,
    @Positive @Schema(description = "Session value", example = "250.00") BigDecimal sessionValue,
    @Positive @Schema(description = "Session duration in minutes", example = "50") Integer sessionDuration,
    @Schema(description = "Accepts health insurance", example = "true") Boolean acceptsInsurance
) {}
