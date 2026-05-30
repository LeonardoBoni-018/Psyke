package com.br.psyke.psyke.ai;

import com.br.psyke.psyke.ai.dto.EvolutionSuggestionRequest;
import com.br.psyke.psyke.ai.dto.EvolutionSuggestionResponse;
import com.br.psyke.psyke.ai.dto.SlotSuggestionRequest;
import com.br.psyke.psyke.ai.dto.SlotSuggestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final ObjectMapper mapper;

    public AiService(
            @Value("${psyke.ai.api-key:}") String apiKey,
            @Value("${psyke.ai.model:gpt-4o-mini}") String model,
            @Value("${psyke.ai.api-url:https://api.openai.com/v1}") String apiUrl,
            @Value("${psyke.ai.enabled:false}") boolean enabled) {
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        this.mapper = new ObjectMapper();
        var builder = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank())
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        this.client = builder.build();
        log.info("AiService initialized: enabled={}, model={}, url={}", enabled, model, apiUrl);
    }

    public EvolutionSuggestionResponse suggestEvolution(EvolutionSuggestionRequest req) {
        if (!enabled || apiKey == null || apiKey.isBlank())
            return new EvolutionSuggestionResponse("", false, "AI disabled. Set psyke.ai.enabled=true and psyke.ai.api-key");

        return callAi(buildEvolutionPrompt(req), EvolutionSuggestionResponse.class);
    }

    public SlotSuggestionResponse suggestSlots(SlotSuggestionRequest req) {
        if (!enabled || apiKey == null || apiKey.isBlank())
            return new SlotSuggestionResponse(List.of(), false, "AI disabled. Set psyke.ai.enabled=true and psyke.ai.api-key");

        return callAi(buildSlotPrompt(req), SlotSuggestionResponse.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T callAi(String prompt, Class<T> responseType) {
        try {
            var requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt()),
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 1024
            );

            var response = client.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null)
                return createErrorResponse(responseType, "No response from AI service");

            var choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty())
                return createErrorResponse(responseType, "No choices in AI response");

            var message = (Map<String, Object>) choices.get(0).get("message");
            var content = (String) message.get("content");

            if (responseType == EvolutionSuggestionResponse.class)
                return (T) new EvolutionSuggestionResponse(content, true, null);
            if (responseType == SlotSuggestionResponse.class)
                return (T) new SlotSuggestionResponse(parseSlotSuggestions(content), true, null);

            return createErrorResponse(responseType, "Unknown response type");
        } catch (Exception e) {
            log.error("AI call failed", e);
            return createErrorResponse(responseType, e.getMessage());
        }
    }

    private String systemPrompt() {
        return """
            You are an AI assistant specialized in psychology clinics.
            Respond in Brazilian Portuguese. Be ethical and professional.
            Never make diagnoses. Only suggest clinical evolution wording based on provided data.
            Be concise and use appropriate technical language.
            """.stripIndent();
    }

    private String buildEvolutionPrompt(EvolutionSuggestionRequest req) {
        return """
            Generate a clinical evolution suggestion for a psychology session:
            - Patient ID: %s
            - Professional ID: %s
            - Session notes: %s
            - Language: %s

            Include: session description, observed progress, techniques used, next session plan.
            3-5 paragraphs in %s.
            """.formatted(req.patientId(), req.professionalId(),
                req.notes() != null ? req.notes() : "N/A",
                req.lang() != null ? req.lang() : "pt-BR",
                req.lang() != null ? req.lang() : "pt-BR");
    }

    private String buildSlotPrompt(SlotSuggestionRequest req) {
        return """
            Suggest optimal appointment slots:
            - Professional: %s
            - Preferred days: %s
            - Time range: %s - %s
            - Duration: %d min
            - From: %s

            Return ONLY a JSON array: [{"date":"yyyy-MM-dd","startTime":"HH:mm","endTime":"HH:mm","score":0.0-1.0}]
            """.formatted(req.professionalId(), req.preferredDays(),
                req.preferredStart() != null ? req.preferredStart() : "08:00",
                req.preferredEnd() != null ? req.preferredEnd() : "18:00",
                req.durationMinutes(), req.from());
    }

    @SuppressWarnings("unchecked")
    private List<SlotSuggestionResponse.Suggestion> parseSlotSuggestions(String content) {
        try {
            var list = mapper.readValue(content, List.class);
            return list.stream().map(item -> {
                var map = (Map<String, Object>) item;
                var score = map.containsKey("score") ? ((Number) map.get("score")).doubleValue() : 0.5;
                return new SlotSuggestionResponse.Suggestion(
                    (String) map.get("date"), (String) map.get("startTime"),
                    (String) map.get("endTime"), score);
            }).toList();
        } catch (Exception e) {
            log.warn("Failed to parse slot suggestions: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createErrorResponse(Class<T> type, String error) {
        if (type == EvolutionSuggestionResponse.class)
            return (T) new EvolutionSuggestionResponse("", false, error);
        if (type == SlotSuggestionResponse.class)
            return (T) new SlotSuggestionResponse(List.of(), false, error);
        return null;
    }
}
