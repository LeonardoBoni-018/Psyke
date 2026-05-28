package com.br.psyke.psyke.dto;

import java.util.UUID;

public record WaitingListResponse(
    UUID id,
    UUID patientId,
    UUID professionalId,
    int priority,
    String requestedAt,
    String status,
    String notifiedAt,
    String offeredUntil
) {}
