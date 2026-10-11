package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreatePatientRequest;
import com.br.psyke.psyke.dto.PatientResponse;
import com.br.psyke.psyke.dto.UpdatePatientRequest;
import com.br.psyke.psyke.exception.DuplicateResourceException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Patient;
import com.br.psyke.psyke.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patients;

    public Page<PatientResponse> findAll(UUID clinicId, Pageable pageable) {
        return patients.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    /** Server-side search by name, e-mail, CPF or phone, optionally filtered by status. */
    public Page<PatientResponse> search(UUID clinicId, String query, String status, Pageable pageable) {
        Specification<Patient> spec = (root, cq, cb) -> cb.equal(root.get("clinicId"), clinicId);
        if (status != null && !status.isBlank()) {
            var st = Patient.PatientStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), st));
        }
        if (query != null && !query.isBlank()) {
            var term = "%" + query.trim().toLowerCase(java.util.Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            var digits = query.replaceAll("\\D", "");
            spec = spec.and((root, cq, cb) -> {
                var byName = cb.like(cb.lower(root.get("fullName")), term, '\\');
                var byEmail = cb.like(cb.lower(root.get("email")), term, '\\');
                if (digits.length() < 3) return cb.or(byName, byEmail);
                var digitTerm = "%" + digits + "%";
                return cb.or(byName, byEmail, cb.like(root.get("cpf"), digitTerm), cb.like(root.get("phone"), digitTerm));
            });
        }
        var sorted = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("fullName"));
        return patients.findAll(spec, sorted).map(this::toResponse);
    }

    public PatientResponse findById(UUID id, UUID clinicId) {
        return patients.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    @Transactional
    public PatientResponse create(CreatePatientRequest req, UUID clinicId) {
        if (req.cpf() != null && !req.cpf().isBlank()
                && patients.existsByClinicIdAndCpf(clinicId, req.cpf()))
            throw new DuplicateResourceException("CPF already registered in this clinic");
        if (req.email() != null && !req.email().isBlank()
                && patients.existsByClinicIdAndEmail(clinicId, req.email()))
            throw new DuplicateResourceException("Email already registered in this clinic");

        var patient = Patient.builder()
                .clinicId(clinicId)
                .fullName(req.fullName())
                .birthDate(req.birthDate())
                .cpf(req.cpf())
                .gender(req.gender())
                .maritalStatus(req.maritalStatus())
                .occupation(req.occupation())
                .phone(req.phone())
                .email(req.email())
                .insurance(req.insurance())
                .insuranceNumber(req.insuranceNumber())
                .referredBy(req.referredBy())
                .build();
        patient = patients.save(patient);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse update(UUID id, UpdatePatientRequest req, UUID clinicId) {
        var patient = patients.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (req.cpf() != null && !req.cpf().isBlank()
                && !req.cpf().equals(patient.getCpf())
                && patients.existsByClinicIdAndCpfAndIdNot(clinicId, req.cpf(), id))
            throw new DuplicateResourceException("CPF already registered in this clinic");
        if (req.email() != null && !req.email().isBlank()
                && !req.email().equals(patient.getEmail())
                && patients.existsByClinicIdAndEmailAndIdNot(clinicId, req.email(), id))
            throw new DuplicateResourceException("Email already registered in this clinic");

        if (req.fullName() != null) patient.setFullName(req.fullName());
        if (req.birthDate() != null) patient.setBirthDate(req.birthDate());
        if (req.cpf() != null) patient.setCpf(req.cpf());
        if (req.gender() != null) patient.setGender(req.gender());
        if (req.maritalStatus() != null) patient.setMaritalStatus(req.maritalStatus());
        if (req.occupation() != null) patient.setOccupation(req.occupation());
        if (req.phone() != null) patient.setPhone(req.phone());
        if (req.email() != null) patient.setEmail(req.email());
        if (req.insurance() != null) patient.setInsurance(req.insurance());
        if (req.insuranceNumber() != null) patient.setInsuranceNumber(req.insuranceNumber());
        if (req.referredBy() != null) patient.setReferredBy(req.referredBy());
        if (req.status() != null) patient.setStatus(Patient.PatientStatus.valueOf(req.status()));

        patient = patients.save(patient);
        return toResponse(patient);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var patient = patients.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        patients.delete(patient);
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(
                p.getId().toString(), p.getFullName(), p.getBirthDate(),
                p.getCpf(), p.getGender(), p.getMaritalStatus(),
                p.getOccupation(), p.getPhone(), p.getEmail(),
                p.getInsurance(), p.getInsuranceNumber(), p.getReferredBy(),
                p.getStatus().name(), p.getFirstAppointment(), p.getLastAppointment(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
