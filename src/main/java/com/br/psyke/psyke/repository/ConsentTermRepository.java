package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.ConsentTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConsentTermRepository extends JpaRepository<ConsentTerm, UUID> {
    Page<ConsentTerm> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<ConsentTerm> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<ConsentTerm> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
}
