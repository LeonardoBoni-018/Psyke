package com.br.psyke.psyke.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record SlotQueryRequest(
    @NotNull String professionalId,
    @NotNull String from,
    @NotNull String to,
    Integer duration
) {}
