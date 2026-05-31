package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreatePrescriptionRequest;
import com.br.psyke.psyke.dto.PrescriptionResponse;
import com.br.psyke.psyke.dto.UpdatePrescriptionRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Prescription;
import com.br.psyke.psyke.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptions;

    public Page<PrescriptionResponse> findAll(UUID clinicId, UUID patientId, Pageable pageable) {
        if (patientId != null)
            return prescriptions.findByClinicIdAndPatientId(clinicId, patientId, pageable).map(this::toResponse);
        return prescriptions.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public PrescriptionResponse findById(UUID id, UUID clinicId) {
        return prescriptions.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
    }

    @Transactional
    public PrescriptionResponse create(CreatePrescriptionRequest req, UUID clinicId) {
        var p = Prescription.builder()
                .clinicId(clinicId)
                .patientId(req.patientId())
                .professionalId(req.professionalId())
                .appointmentId(req.appointmentId())
                .medication(req.medication())
                .dosage(req.dosage())
                .instructions(req.instructions())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();
        p = prescriptions.save(p);
        return toResponse(p);
    }

    @Transactional
    public PrescriptionResponse update(UUID id, UpdatePrescriptionRequest req, UUID clinicId) {
        var p = prescriptions.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        if (req.medication() != null) p.setMedication(req.medication());
        if (req.dosage() != null) p.setDosage(req.dosage());
        if (req.instructions() != null) p.setInstructions(req.instructions());
        if (req.startDate() != null) p.setStartDate(req.startDate());
        if (req.endDate() != null) p.setEndDate(req.endDate());
        if (req.active() != null) p.setActive(req.active());

        p = prescriptions.save(p);
        return toResponse(p);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var p = prescriptions.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        prescriptions.delete(p);
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return new PrescriptionResponse(
                p.getId().toString(), p.getClinicId(), p.getPatientId(),
                p.getProfessionalId(), p.getAppointmentId(),
                p.getMedication(), p.getDosage(), p.getInstructions(),
                p.getStartDate(), p.getEndDate(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
