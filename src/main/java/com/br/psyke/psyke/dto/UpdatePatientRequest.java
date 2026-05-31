package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Request to update an existing patient")
public record UpdatePatientRequest(
    @Size(max = 300) @Schema(description = "Full name") String fullName,
    @Schema(description = "Birth date", example = "1990-05-15") LocalDate birthDate,
    @Size(max = 14) @Schema(description = "CPF (unique per clinic)", example = "12345678900") String cpf,
    @Size(max = 30) @Schema(description = "Gender", example = "Masculino") String gender,
    @Size(max = 30) @Schema(description = "Marital status", example = "Solteiro") String maritalStatus,
    @Size(max = 200) @Schema(description = "Occupation", example = "Engenheiro") String occupation,
    @Size(max = 20) @Schema(description = "Phone", example = "+5511988887777") String phone,
    @Email @Size(max = 255) @Schema(description = "Email (unique per clinic)", example = "joao@example.com") String email,
    @Size(max = 100) @Schema(description = "Health insurance", example = "Unimed") String insurance,
    @Size(max = 50) @Schema(description = "Insurance membership number") String insuranceNumber,
    @Size(max = 200) @Schema(description = "Who referred the patient") String referredBy,
    @Size(max = 30) @Schema(description = "Status", example = "ACTIVE") String status
) {}
