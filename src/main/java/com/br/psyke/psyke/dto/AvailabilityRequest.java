package com.br.psyke.psyke.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record AvailabilityRequest(
    @NotNull Integer dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull Integer sessionDuration
) {}
