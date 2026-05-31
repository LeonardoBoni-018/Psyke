package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Invoice> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<Invoice> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
    Page<Invoice> findByClinicIdAndStatus(UUID clinicId, Invoice.InvoiceStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.clinicId = :clinicId AND i.status = :status")
    BigDecimal sumByClinicIdAndStatus(@Param("clinicId") UUID clinicId, @Param("status") Invoice.InvoiceStatus status);
}
