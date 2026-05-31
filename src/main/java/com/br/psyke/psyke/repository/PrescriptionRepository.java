package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    Page<Prescription> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Prescription> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<Prescription> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
}
