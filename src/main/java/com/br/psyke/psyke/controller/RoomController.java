package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.CreateRoomRequest;
import com.br.psyke.psyke.dto.RoomResponse;
import com.br.psyke.psyke.dto.UpdateRoomRequest;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management within the current clinic")
public class RoomController {

    private final RoomService service;

    @GetMapping
    @Operation(summary = "List active rooms")
    public ResponseEntity<List<RoomResponse>> findAllActive() {
        return ResponseEntity.ok(service.findAllActive(
                UUID.fromString(TenantContext.getClinicId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<RoomResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Create a new room")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, UUID.fromString(TenantContext.getClinicId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Update an existing room")
    public ResponseEntity<RoomResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a room (admin only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
