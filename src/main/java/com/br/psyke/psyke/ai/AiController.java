package com.br.psyke.psyke.ai;

import com.br.psyke.psyke.ai.dto.EvolutionSuggestionRequest;
import com.br.psyke.psyke.ai.dto.EvolutionSuggestionResponse;
import com.br.psyke.psyke.ai.dto.SlotSuggestionRequest;
import com.br.psyke.psyke.ai.dto.SlotSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "AI-powered features: clinical evolution generation and smart scheduling suggestions")
public class AiController {

    private final AiService service;

    @PostMapping("/evolutions/suggest")
    @Operation(summary = "Suggest clinical evolution text", description = "Uses AI to generate a clinical evolution note from session data. Requires psyke.ai.enabled=true and an API key.")
    public ResponseEntity<EvolutionSuggestionResponse> suggestEvolution(@Valid @RequestBody EvolutionSuggestionRequest req) {
        return ResponseEntity.ok(service.suggestEvolution(req));
    }

    @PostMapping("/slots/suggest")
    @Operation(summary = "Suggest optimal appointment slots", description = "Uses AI to suggest the best time slots for scheduling based on preferences. Requires psyke.ai.enabled=true and an API key.")
    public ResponseEntity<SlotSuggestionResponse> suggestSlots(@Valid @RequestBody SlotSuggestionRequest req) {
        return ResponseEntity.ok(service.suggestSlots(req));
    }
}
