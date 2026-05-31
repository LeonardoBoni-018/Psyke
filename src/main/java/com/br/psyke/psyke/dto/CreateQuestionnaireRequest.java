package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a questionnaire template")
public record CreateQuestionnaireRequest(
    @NotBlank @Size(max = 200) @Schema(description = "Questionnaire title", example = "GAD-7") String title,
    @Schema(description = "Description", example = "Generalized Anxiety Disorder assessment") String description,
    @Schema(description = "Questions as JSON array", example = "[{\"question\":\"Feeling nervous?\",\"type\":\"scale\",\"min\":0,\"max\":3}]") String questions
) {}
