package com.br.psyke.psyke.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecurrenceRequest(
    @NotNull UUID patientId,
    @NotNull UUID professionalId,
    UUID roomId,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    String notes,
    String freq,
    Integer interval,
    Integer count,
    LocalDate until
) {}
