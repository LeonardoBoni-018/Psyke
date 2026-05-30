package com.br.psyke.psyke.dto.prontuario;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProntuarioRequest(
    @NotNull UUID patientId,
    String allergies,
    String chronicConditions,
    String medications,
    String notes
) {}
