package com.br.psyke.psyke.dto;

import java.util.UUID;

public record ExceptionResponse(
    UUID id,
    UUID professionalId,
    String exceptionDate,
    String startTime,
    String endTime,
    String type,
    String reason,
    boolean active
) {}
