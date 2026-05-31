package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateNotificationConfigRequest;
import com.br.psyke.psyke.dto.NotificationConfigResponse;
import com.br.psyke.psyke.dto.UpdateNotificationConfigRequest;
import com.br.psyke.psyke.service.NotificationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/notification-configs")
@RequiredArgsConstructor
@Tag(name = "Notification Configs", description = "Per-user notification preference management")
public class NotificationConfigController {

    private final NotificationConfigService service;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notification config by user ID")
    public ResponseEntity<NotificationConfigResponse> findByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.findByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Create notification config for a user")
    public ResponseEntity<NotificationConfigResponse> create(@Valid @RequestBody CreateNotificationConfigRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notification config")
    public ResponseEntity<NotificationConfigResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNotificationConfigRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification config")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
