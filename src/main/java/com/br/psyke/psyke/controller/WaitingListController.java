package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.WaitingListRequest;
import com.br.psyke.psyke.dto.WaitingListResponse;
import com.br.psyke.psyke.service.WaitingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/waiting-list")
@RequiredArgsConstructor
public class WaitingListController {

    private final WaitingListService service;

    @PostMapping
    public ResponseEntity<WaitingListResponse> join(@Valid @RequestBody WaitingListRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.join(req));
    }

    @GetMapping
    public ResponseEntity<List<WaitingListResponse>> listByProfessional(
            @RequestParam UUID professionalId) {
        return ResponseEntity.ok(service.listByProfessional(professionalId));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptOffer(@PathVariable UUID id) {
        service.acceptOffer(id);
        return ResponseEntity.ok().build();
    }
}
