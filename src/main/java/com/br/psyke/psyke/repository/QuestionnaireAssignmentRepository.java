package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.QuestionnaireAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireAssignmentRepository extends JpaRepository<QuestionnaireAssignment, UUID> {
    Page<QuestionnaireAssignment> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<QuestionnaireAssignment> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<QuestionnaireAssignment> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
}
