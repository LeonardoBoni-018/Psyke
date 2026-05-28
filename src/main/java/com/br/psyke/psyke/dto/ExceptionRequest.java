package com.br.psyke.psyke.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExceptionRequest(
    @NotNull LocalDate exceptionDate,
    LocalTime startTime,
    LocalTime endTime,
    @NotNull String type,
    String reason
) {}
