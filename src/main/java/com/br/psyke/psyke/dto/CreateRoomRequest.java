package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new room")
public record CreateRoomRequest(
    @NotBlank @Size(max = 200) @Schema(description = "Room name", example = "Sala 01") String name,
    @Size(max = 50) @Schema(description = "Display color", example = "#00FF00") String color,
    @Positive @Schema(description = "Capacity", example = "10") Integer capacity,
    @Schema(description = "Room type: PHYSICAL or VIRTUAL", example = "PHYSICAL") String type
) {}
