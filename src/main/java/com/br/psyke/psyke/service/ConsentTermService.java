package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.ConsentTermResponse;
import com.br.psyke.psyke.dto.CreateConsentTermRequest;
import com.br.psyke.psyke.dto.UpdateConsentTermRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.ConsentTerm;
import com.br.psyke.psyke.repository.ConsentTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentTermService {

    private final ConsentTermRepository terms;

    public Page<ConsentTermResponse> findAll(UUID clinicId, UUID patientId, Pageable pageable) {
        if (patientId != null)
            return terms.findByClinicIdAndPatientId(clinicId, patientId, pageable).map(this::toResponse);
        return terms.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public ConsentTermResponse findById(UUID id, UUID clinicId) {
        return terms.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Consent term not found"));
    }

    @Transactional
    public ConsentTermResponse create(CreateConsentTermRequest req, UUID clinicId) {
        var t = ConsentTerm.builder()
                .clinicId(clinicId)
                .patientId(req.patientId())
                .professionalId(req.professionalId())
                .title(req.title())
                .content(req.content())
                .expiresAt(req.expiresAt())
                .build();
        t = terms.save(t);
        return toResponse(t);
    }

    @Transactional
    public ConsentTermResponse sign(UUID id, UUID clinicId, boolean asProfessional) {
        var t = terms.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent term not found"));
        if (asProfessional) t.setSignedByProfessional(true);
        else t.setSignedByPatient(true);
        if (t.isSignedByPatient() && t.isSignedByProfessional())
            t.setSignedAt(OffsetDateTime.now());
        t = terms.save(t);
        return toResponse(t);
    }

    @Transactional
    public ConsentTermResponse update(UUID id, UpdateConsentTermRequest req, UUID clinicId) {
        var t = terms.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent term not found"));
        if (req.title() != null) t.setTitle(req.title());
        if (req.content() != null) t.setContent(req.content());
        if (req.signedByPatient() != null) t.setSignedByPatient(req.signedByPatient());
        if (req.signedByProfessional() != null) t.setSignedByProfessional(req.signedByProfessional());
        if (req.active() != null) t.setActive(req.active());
        if (t.isSignedByPatient() && t.isSignedByProfessional()) t.setSignedAt(OffsetDateTime.now());
        t = terms.save(t);
        return toResponse(t);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var t = terms.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent term not found"));
        terms.delete(t);
    }

    private ConsentTermResponse toResponse(ConsentTerm t) {
        return new ConsentTermResponse(
                t.getId().toString(), t.getClinicId(), t.getPatientId(),
                t.getProfessionalId(), t.getTitle(), t.getContent(),
                t.isSignedByPatient(), t.isSignedByProfessional(),
                t.getSignedAt(), t.getExpiresAt(), t.isActive(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
