package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "Patient profile response")
public record PatientResponse(
    String id,
    String fullName,
    LocalDate birthDate,
    String cpf,
    String gender,
    String maritalStatus,
    String occupation,
    String phone,
    String email,
    String insurance,
    String insuranceNumber,
    String referredBy,
    String status,
    OffsetDateTime firstAppointment,
    OffsetDateTime lastAppointment,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
