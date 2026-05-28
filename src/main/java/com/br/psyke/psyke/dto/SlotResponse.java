package com.br.psyke.psyke.dto;

import java.util.UUID;

public record SlotResponse(
    UUID id,
    UUID professionalId,
    String startTime,
    String endTime,
    String status
) {}
