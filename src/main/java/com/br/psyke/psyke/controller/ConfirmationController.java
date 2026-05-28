package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.ConfirmationResponse;
import com.br.psyke.psyke.service.ConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ConfirmationController {

    private final ConfirmationService service;

    @GetMapping("/confirm/{token}")
    public ResponseEntity<ConfirmationResponse> confirm(@PathVariable UUID token) {
        return ResponseEntity.ok(service.confirm(token));
    }

    @GetMapping("/cancel/{token}")
    public ResponseEntity<ConfirmationResponse> cancel(@PathVariable UUID token) {
        return ResponseEntity.ok(service.cancel(token));
    }
}
