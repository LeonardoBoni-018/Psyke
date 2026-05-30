package com.br.psyke.psyke.dto.prontuario;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EvolucaoClinicaRequest(
    @NotNull UUID appointmentId,
    String subjective,
    String objective,
    String assessment,
    String plan,
    String techniques
) {}
