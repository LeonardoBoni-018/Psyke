package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.ClinicResponse;
import com.br.psyke.psyke.dto.CreateClinicRequest;
import com.br.psyke.psyke.dto.UpdateClinicRequest;
import com.br.psyke.psyke.exception.DuplicateResourceException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Clinic;
import com.br.psyke.psyke.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinics;

    public Page<ClinicResponse> findAll(Pageable pageable) {
        return clinics.findAll(pageable).map(this::toResponse);
    }

    public ClinicResponse findById(UUID id) {
        return clinics.findByIdAndActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));
    }

    @Transactional
    public ClinicResponse create(CreateClinicRequest req) {
        if (req.cnpj() != null && !req.cnpj().isBlank()
                && clinics.existsByCnpj(req.cnpj()))
            throw new DuplicateResourceException("CNPJ already registered");

        var clinic = Clinic.builder()
                .name(req.name())
                .legalName(req.legalName())
                .cnpj(req.cnpj())
                .crp(req.crp())
                .address(req.address())
                .phone(req.phone())
                .logoUrl(req.logoUrl())
                .settings(req.settings() != null ? req.settings() : "{}")
                .build();
        clinic = clinics.save(clinic);
        return toResponse(clinic);
    }

    @Transactional
    public ClinicResponse update(UUID id, UpdateClinicRequest req) {
        var clinic = clinics.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));

        if (req.cnpj() != null && !req.cnpj().isBlank()
                && !req.cnpj().equals(clinic.getCnpj())
                && clinics.existsByCnpjAndIdNot(req.cnpj(), id))
            throw new DuplicateResourceException("CNPJ already registered");

        if (req.name() != null) clinic.setName(req.name());
        if (req.legalName() != null) clinic.setLegalName(req.legalName());
        if (req.cnpj() != null) clinic.setCnpj(req.cnpj());
        if (req.crp() != null) clinic.setCrp(req.crp());
        if (req.address() != null) clinic.setAddress(req.address());
        if (req.phone() != null) clinic.setPhone(req.phone());
        if (req.logoUrl() != null) clinic.setLogoUrl(req.logoUrl());
        if (req.settings() != null) clinic.setSettings(req.settings());
        if (req.active() != null) clinic.setActive(req.active());

        clinic = clinics.save(clinic);
        return toResponse(clinic);
    }

    @Transactional
    public void delete(UUID id) {
        var clinic = clinics.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found"));
        clinic.setActive(false);
        clinics.save(clinic);
    }

    private ClinicResponse toResponse(Clinic c) {
        return new ClinicResponse(
                c.getId().toString(), c.getName(), c.getLegalName(),
                c.getCnpj(), c.getCrp(), c.getAddress(),
                c.getPhone(), c.getLogoUrl(), c.getSettings(),
                c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
