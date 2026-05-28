package com.br.psyke.psyke.dto;

import java.util.UUID;

public record AvailabilityResponse(
    UUID id,
    int dayOfWeek,
    String startTime,
    String endTime,
    int sessionDuration,
    boolean active
) {}
