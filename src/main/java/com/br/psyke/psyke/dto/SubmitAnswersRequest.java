package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request to submit answers to a questionnaire assignment")
public record SubmitAnswersRequest(
    @NotNull @Schema(description = "Answers as JSON", example = "[{\"questionIndex\":0,\"answer\":2}]") String answers,
    @Schema(description = "Calculated score", example = "15.00") BigDecimal score
) {}
