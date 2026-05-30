package com.br.psyke.psyke.dto.prontuario;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AnamneseRequest(
    @NotNull UUID prontuarioId,
    String chiefComplaint,
    String historyIllness,
    String personalHistory,
    String familyHistory,
    String socialHistory,
    String medications
) {}
