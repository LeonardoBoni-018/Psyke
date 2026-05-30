package com.br.psyke.psyke.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to generate a clinical evolution note using AI")
public record EvolutionSuggestionRequest(
    @NotNull @Schema(description = "Patient ID") UUID patientId,
    @NotNull @Schema(description = "Professional ID") UUID professionalId,
    @Schema(description = "Session notes / keywords to include", example = "Paciente relatou melhora na ansiedade. Exercícios de respiração foram eficazes.") String notes,
    @Schema(description = "Language for the generated text (pt-BR or en)", example = "pt-BR") String lang
) {}
