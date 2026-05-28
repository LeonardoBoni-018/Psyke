package com.br.psyke.psyke.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentEvent(
    String eventType,
    UUID appointmentId,
    UUID clinicId,
    UUID patientId,
    UUID professionalId,
    UUID roomId,
    String startTime,
    String endTime,
    String status,
    LocalDateTime timestamp
) {}
