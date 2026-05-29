package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "FullCalendar-compatible event resource")
public record CalendarEvent(
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
    @Schema(example = "Paciente - Dr. Profissional") String title,
    @Schema(example = "2026-05-28T09:00:00") String start,
    @Schema(example = "2026-05-28T09:50:00") String end,
    @Schema(example = "#3788d8") String backgroundColor,
    @Schema(example = "#3788d8") String borderColor,
    @Schema(example = "#ffffff") String textColor,
    Map<String, Object> extendedProps
) {}
