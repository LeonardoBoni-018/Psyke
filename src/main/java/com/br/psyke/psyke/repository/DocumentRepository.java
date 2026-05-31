package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Document> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<Document> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
}
