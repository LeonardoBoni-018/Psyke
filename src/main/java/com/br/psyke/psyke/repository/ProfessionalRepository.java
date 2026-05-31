package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Professional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
    Page<Professional> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Professional> findByIdAndClinicId(UUID id, UUID clinicId);
    boolean existsByClinicIdAndCrp(UUID clinicId, String crp);
    boolean existsByClinicIdAndCrpAndIdNot(UUID clinicId, String crp, UUID id);
    long countByClinicIdAndActiveTrue(UUID clinicId);
}
