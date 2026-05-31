package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateProfessionalRequest;
import com.br.psyke.psyke.dto.ProfessionalResponse;
import com.br.psyke.psyke.dto.UpdateProfessionalRequest;
import com.br.psyke.psyke.exception.DuplicateResourceException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Professional;
import com.br.psyke.psyke.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionals;

    public Page<ProfessionalResponse> findAll(UUID clinicId, Pageable pageable) {
        return professionals.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public ProfessionalResponse findById(UUID id, UUID clinicId) {
        return professionals.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
    }

    @Transactional
    public ProfessionalResponse create(CreateProfessionalRequest req, UUID clinicId) {
        if (professionals.existsByClinicIdAndCrp(clinicId, req.crp()))
            throw new DuplicateResourceException("CRP already registered in this clinic");

        var professional = Professional.builder()
                .clinicId(clinicId)
                .crp(req.crp())
                .fullName(req.fullName())
                .specialty(req.specialty())
                .approach(req.approach())
                .resume(req.resume())
                .sessionValue(req.sessionValue())
                .sessionDuration(req.sessionDuration() != null ? req.sessionDuration() : 50)
                .acceptsInsurance(req.acceptsInsurance() != null ? req.acceptsInsurance() : false)
                .build();
        professional = professionals.save(professional);
        return toResponse(professional);
    }

    @Transactional
    public ProfessionalResponse update(UUID id, UpdateProfessionalRequest req, UUID clinicId) {
        var professional = professionals.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        if (req.crp() != null && !req.crp().isBlank()
                && !req.crp().equals(professional.getCrp())
                && professionals.existsByClinicIdAndCrpAndIdNot(clinicId, req.crp(), id))
            throw new DuplicateResourceException("CRP already registered in this clinic");

        if (req.fullName() != null) professional.setFullName(req.fullName());
        if (req.crp() != null) professional.setCrp(req.crp());
        if (req.specialty() != null) professional.setSpecialty(req.specialty());
        if (req.approach() != null) professional.setApproach(req.approach());
        if (req.resume() != null) professional.setResume(req.resume());
        if (req.sessionValue() != null) professional.setSessionValue(req.sessionValue());
        if (req.sessionDuration() != null) professional.setSessionDuration(req.sessionDuration());
        if (req.acceptsInsurance() != null) professional.setAcceptsInsurance(req.acceptsInsurance());
        if (req.active() != null) professional.setActive(req.active());

        professional = professionals.save(professional);
        return toResponse(professional);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var professional = professionals.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));
        professionals.delete(professional);
    }

    private ProfessionalResponse toResponse(Professional p) {
        return new ProfessionalResponse(
                p.getId().toString(), p.getFullName(), p.getCrp(),
                p.getSpecialty(), p.getApproach(), p.getResume(),
                p.getSessionValue(), p.getSessionDuration(),
                p.isAcceptsInsurance(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
