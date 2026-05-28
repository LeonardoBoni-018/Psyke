package com.br.psyke.psyke.dto;

import java.util.UUID;

public record ConfirmationResponse(
    String status,
    UUID appointmentId,
    UUID token
) {}
