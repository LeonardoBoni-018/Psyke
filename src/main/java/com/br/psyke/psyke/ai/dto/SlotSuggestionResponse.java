package com.br.psyke.psyke.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI-suggested appointment slot")
public record SlotSuggestionResponse(
    @Schema(description = "List of suggested time slots") List<Suggestion> suggestions,
    @Schema(description = "Whether AI suggestion succeeded", example = "true") boolean success,
    @Schema(description = "Error message if generation failed") String error
) {
    public record Suggestion(
        @Schema(example = "2026-06-02") String date,
        @Schema(example = "09:00") String startTime,
        @Schema(example = "09:50") String endTime,
        @Schema(example = "0.85") double score
    ) {}
}
