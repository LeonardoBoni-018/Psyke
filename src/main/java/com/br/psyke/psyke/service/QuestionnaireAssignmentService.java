package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.QuestionnaireAssignmentResponse;
import com.br.psyke.psyke.dto.SubmitAnswersRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.QuestionnaireAssignment;
import com.br.psyke.psyke.repository.QuestionnaireAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionnaireAssignmentService {

    private final QuestionnaireAssignmentRepository assignments;

    public Page<QuestionnaireAssignmentResponse> findAll(UUID clinicId, UUID patientId, Pageable pageable) {
        if (patientId != null)
            return assignments.findByClinicIdAndPatientId(clinicId, patientId, pageable).map(this::toResponse);
        return assignments.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public QuestionnaireAssignmentResponse findById(UUID id, UUID clinicId) {
        return assignments.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }

    @Transactional
    public QuestionnaireAssignmentResponse assign(UUID clinicId, UUID questionnaireId, UUID patientId, UUID professionalId) {
        var a = QuestionnaireAssignment.builder()
                .clinicId(clinicId)
                .questionnaireId(questionnaireId)
                .patientId(patientId)
                .professionalId(professionalId)
                .build();
        a = assignments.save(a);
        return toResponse(a);
    }

    @Transactional
    public QuestionnaireAssignmentResponse submit(UUID id, UUID clinicId, SubmitAnswersRequest req) {
        var a = assignments.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (a.getStatus() == QuestionnaireAssignment.AssignmentStatus.COMPLETED)
            throw new IllegalStateException("Assignment already completed");

        a.setAnswers(req.answers());
        a.setScore(req.score());
        a.setStatus(QuestionnaireAssignment.AssignmentStatus.COMPLETED);
        a.setCompletedAt(OffsetDateTime.now());
        a = assignments.save(a);
        return toResponse(a);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var a = assignments.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        assignments.delete(a);
    }

    private QuestionnaireAssignmentResponse toResponse(QuestionnaireAssignment a) {
        return new QuestionnaireAssignmentResponse(
                a.getId().toString(), a.getClinicId(), a.getQuestionnaireId(),
                a.getPatientId(), a.getProfessionalId(),
                a.getStatus().name(), a.getAnswers(), a.getScore(),
                a.getAssignedAt(), a.getCompletedAt(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
