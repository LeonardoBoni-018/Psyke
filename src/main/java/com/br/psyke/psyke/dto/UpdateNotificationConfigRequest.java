package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update notification config")
public record UpdateNotificationConfigRequest(
    @Schema(description = "Enable email notifications", example = "true") Boolean emailEnabled,
    @Schema(description = "Enable SMS notifications", example = "false") Boolean smsEnabled,
    @Schema(description = "Enable push notifications", example = "false") Boolean pushEnabled,
    @Schema(description = "Minutes before appointment to remind", example = "1440") Integer remindBeforeMinutes,
    @Schema(description = "Notify on cancellations", example = "true") Boolean notifyCancellations,
    @Schema(description = "Notify on confirmations", example = "true") Boolean notifyConfirmations,
    @Schema(description = "Notify on changes", example = "true") Boolean notifyChanges
) {}
