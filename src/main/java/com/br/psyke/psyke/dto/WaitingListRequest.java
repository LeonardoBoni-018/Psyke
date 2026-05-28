package com.br.psyke.psyke.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WaitingListRequest(
    @NotNull UUID patientId,
    @NotNull UUID professionalId,
    Integer priority
) {}
