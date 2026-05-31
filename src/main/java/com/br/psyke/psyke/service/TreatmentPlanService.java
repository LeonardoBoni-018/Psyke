package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateTreatmentPlanRequest;
import com.br.psyke.psyke.dto.TreatmentPlanResponse;
import com.br.psyke.psyke.dto.UpdateTreatmentPlanRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.TreatmentPlan;
import com.br.psyke.psyke.repository.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TreatmentPlanService {

    private final TreatmentPlanRepository plans;

    public Page<TreatmentPlanResponse> findAll(UUID clinicId, UUID prontuarioId, Pageable pageable) {
        if (prontuarioId != null)
            return plans.findByClinicIdAndProntuarioId(clinicId, prontuarioId, pageable).map(this::toResponse);
        return plans.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public TreatmentPlanResponse findById(UUID id, UUID clinicId) {
        return plans.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment plan not found"));
    }

    @Transactional
    public TreatmentPlanResponse create(CreateTreatmentPlanRequest req, UUID clinicId) {
        var plan = TreatmentPlan.builder()
                .clinicId(clinicId)
                .prontuarioId(req.prontuarioId())
                .title(req.title())
                .description(req.description())
                .goals(req.goals())
                .techniques(req.techniques())
                .frequency(req.frequency())
                .estimatedSessions(req.estimatedSessions())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();
        plan = plans.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public TreatmentPlanResponse update(UUID id, UpdateTreatmentPlanRequest req, UUID clinicId) {
        var plan = plans.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment plan not found"));

        if (req.title() != null) plan.setTitle(req.title());
        if (req.description() != null) plan.setDescription(req.description());
        if (req.goals() != null) plan.setGoals(req.goals());
        if (req.techniques() != null) plan.setTechniques(req.techniques());
        if (req.frequency() != null) plan.setFrequency(req.frequency());
        if (req.estimatedSessions() != null) plan.setEstimatedSessions(req.estimatedSessions());
        if (req.status() != null) plan.setStatus(TreatmentPlan.PlanStatus.valueOf(req.status()));
        if (req.startDate() != null) plan.setStartDate(req.startDate());
        if (req.endDate() != null) plan.setEndDate(req.endDate());

        plan = plans.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var plan = plans.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment plan not found"));
        plans.delete(plan);
    }

    private TreatmentPlanResponse toResponse(TreatmentPlan p) {
        return new TreatmentPlanResponse(
                p.getId().toString(), p.getClinicId(), p.getProntuarioId(),
                p.getTitle(), p.getDescription(), p.getGoals(),
                p.getTechniques(), p.getFrequency(), p.getEstimatedSessions(),
                p.getStatus().name(), p.getStartDate(), p.getEndDate(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
