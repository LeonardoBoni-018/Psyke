package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.WaitingListRequest;
import com.br.psyke.psyke.dto.WaitingListResponse;
import com.br.psyke.psyke.service.WaitingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<Page<WaitingListResponse>> listByProfessional(
            @RequestParam UUID professionalId, Pageable pageable) {
        return ResponseEntity.ok(service.listByProfessional(professionalId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        service.remove(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptOffer(@PathVariable UUID id) {
        service.acceptOffer(id);
        return ResponseEntity.ok().build();
    }
}
