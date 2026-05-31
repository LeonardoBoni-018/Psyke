package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.TreatmentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {
    Page<TreatmentPlan> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<TreatmentPlan> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<TreatmentPlan> findByClinicIdAndProntuarioId(UUID clinicId, UUID prontuarioId, Pageable pageable);
}
