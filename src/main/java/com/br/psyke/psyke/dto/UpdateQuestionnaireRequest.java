package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update a questionnaire template")
public record UpdateQuestionnaireRequest(
    @Size(max = 200) @Schema(description = "Questionnaire title", example = "GAD-7") String title,
    @Schema(description = "Description") String description,
    @Schema(description = "Questions as JSON array") String questions,
    @Schema(description = "Whether the questionnaire is active", example = "true") Boolean active
) {}
