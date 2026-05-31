package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Room response")
public record RoomResponse(
    String id,
    String name,
    String color,
    int capacity,
    String type,
    boolean active,
    LocalDateTime createdAt
) {}
