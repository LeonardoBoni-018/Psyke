package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateQuestionnaireRequest;
import com.br.psyke.psyke.dto.QuestionnaireResponse;
import com.br.psyke.psyke.dto.UpdateQuestionnaireRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Questionnaire;
import com.br.psyke.psyke.repository.QuestionnaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaires;

    public Page<QuestionnaireResponse> findAll(UUID clinicId, Pageable pageable) {
        return questionnaires.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public QuestionnaireResponse findById(UUID id, UUID clinicId) {
        return questionnaires.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire not found"));
    }

    @Transactional
    public QuestionnaireResponse create(CreateQuestionnaireRequest req, UUID clinicId) {
        var q = Questionnaire.builder()
                .clinicId(clinicId)
                .title(req.title())
                .description(req.description())
                .questions(req.questions() != null ? req.questions() : "[]")
                .build();
        q = questionnaires.save(q);
        return toResponse(q);
    }

    @Transactional
    public QuestionnaireResponse update(UUID id, UpdateQuestionnaireRequest req, UUID clinicId) {
        var q = questionnaires.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire not found"));

        if (req.title() != null) q.setTitle(req.title());
        if (req.description() != null) q.setDescription(req.description());
        if (req.questions() != null) q.setQuestions(req.questions());
        if (req.active() != null) q.setActive(req.active());

        q = questionnaires.save(q);
        return toResponse(q);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var q = questionnaires.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire not found"));
        questionnaires.delete(q);
    }

    private QuestionnaireResponse toResponse(Questionnaire q) {
        return new QuestionnaireResponse(
                q.getId().toString(), q.getClinicId(), q.getTitle(),
                q.getDescription(), q.getQuestions(), q.isActive(),
                q.getCreatedAt(), q.getUpdatedAt());
    }
}
