package com.br.psyke.psyke.dto;

import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID clinicId,
    UUID patientId,
    UUID professionalId,
    UUID roomId,
    String startTime,
    String endTime,
    String status,
    String prontuario,
    String notes,
    String createdAt,
    String updatedAt
) {}
