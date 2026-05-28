package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.AppointmentResponse;
import com.br.psyke.psyke.dto.RecurrenceRequest;
import com.br.psyke.psyke.service.RecurrenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/appointments/recurring")
@RequiredArgsConstructor
public class RecurrenceController {

    private final RecurrenceService service;

    @PostMapping
    public ResponseEntity<List<AppointmentResponse>> createRecurring(@Valid @RequestBody RecurrenceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRecurring(req));
    }
}
