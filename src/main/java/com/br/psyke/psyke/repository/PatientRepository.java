package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Page<Patient> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Patient> findByIdAndClinicId(UUID id, UUID clinicId);
    boolean existsByClinicIdAndCpf(UUID clinicId, String cpf);
    boolean existsByClinicIdAndEmail(UUID clinicId, String email);
    boolean existsByClinicIdAndCpfAndIdNot(UUID clinicId, String cpf, UUID id);
    boolean existsByClinicIdAndEmailAndIdNot(UUID clinicId, String email, UUID id);
    long countByClinicId(UUID clinicId);
}
