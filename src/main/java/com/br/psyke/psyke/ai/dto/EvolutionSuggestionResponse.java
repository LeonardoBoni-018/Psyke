package com.br.psyke.psyke.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI-generated clinical evolution suggestion")
public record EvolutionSuggestionResponse(
    @Schema(description = "Generated evolution text", example = "Evolução clínica gerada por IA...") String suggestedText,
    @Schema(description = "Whether AI generation succeeded", example = "true") boolean success,
    @Schema(description = "Error message if generation failed") String error
) {}
