package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Notification config response")
public record NotificationConfigResponse(
    String id,
    UUID userId,
    boolean emailEnabled,
    boolean smsEnabled,
    boolean pushEnabled,
    int remindBeforeMinutes,
    boolean notifyCancellations,
    boolean notifyConfirmations,
    boolean notifyChanges,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
