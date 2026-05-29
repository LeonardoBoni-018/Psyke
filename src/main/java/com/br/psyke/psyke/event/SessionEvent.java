package com.br.psyke.psyke.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionEvent(
    UUID appointmentId,
    UUID clinicId,
    UUID patientId,
    UUID professionalId,
    String startTime,
    String endTime,
    String notes,
    String prontuario,
    LocalDateTime timestamp
) {}
