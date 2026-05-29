package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a session from a confirmed appointment")
public record CreateSessionRequest(
    @Schema(description = "Medical notes / prontuario content", example = "Patient reported anxiety reduction. Next session scheduled for next week.")
    String prontuario
) {}
