package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new clinic")
public record CreateClinicRequest(
    @NotBlank @Size(max = 200) @Schema(description = "Clinic name", example = "Psyke Centro Psicológico") String name,
    @Size(max = 200) @Schema(description = "Legal name", example = "Psyke Serviços Psicológicos Ltda") String legalName,
    @Size(max = 18) @Schema(description = "CNPJ (unique)", example = "11222333000181") String cnpj,
    @Size(max = 20) @Schema(description = "CRP registration", example = "06/12345") String crp,
    @Schema(description = "Full address") String address,
    @Size(max = 20) @Schema(description = "Phone", example = "+5511988887777") String phone,
    @Size(max = 500) @Schema(description = "Logo URL") String logoUrl,
    @Schema(description = "Settings as JSON", example = "{}") String settings
) {}
