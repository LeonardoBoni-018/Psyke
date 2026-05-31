package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Invoice> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<Invoice> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
    Page<Invoice> findByClinicIdAndStatus(UUID clinicId, Invoice.InvoiceStatus status, Pageable pageable);
}
