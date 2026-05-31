package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Questionnaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {
    Page<Questionnaire> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Questionnaire> findByIdAndClinicId(UUID id, UUID clinicId);
}
