package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Invoice> findByIdAndClinicId(UUID id, UUID clinicId);
    Page<Invoice> findByClinicIdAndPatientId(UUID clinicId, UUID patientId, Pageable pageable);
    Page<Invoice> findByClinicIdAndStatus(UUID clinicId, Invoice.InvoiceStatus status, Pageable pageable);
    Page<Invoice> findByClinicIdAndPatientIdAndStatus(UUID clinicId, UUID patientId, Invoice.InvoiceStatus status, Pageable pageable);

    List<Invoice> findByClinicIdAndCreatedAtBetweenOrderByCreatedAt(UUID clinicId, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.clinicId = :clinicId AND i.status = :status")
    BigDecimal sumByClinicIdAndStatus(@Param("clinicId") UUID clinicId, @Param("status") Invoice.InvoiceStatus status);

    /** Paid amount whose payment (or last update, when no payment date was recorded) falls in [from, to). */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.clinicId = :clinicId AND i.status = 'PAID' " +
           "AND COALESCE(i.paymentDate, i.updatedAt) >= :from AND COALESCE(i.paymentDate, i.updatedAt) < :to")
    BigDecimal sumPaidBetween(@Param("clinicId") UUID clinicId,
                              @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** Rows of [status, count, sum] for invoices issued in [from, to). */
    @Query("SELECT i.status, COUNT(i), COALESCE(SUM(i.amount), 0) FROM Invoice i " +
           "WHERE i.clinicId = :clinicId AND i.createdAt >= :from AND i.createdAt < :to GROUP BY i.status")
    List<Object[]> summarizeIssuedBetween(@Param("clinicId") UUID clinicId,
                                          @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** Rows of [yyyy-MM, sum] of paid revenue per month in [from, to). */
    @Query(value = "SELECT to_char(date_trunc('month', COALESCE(payment_date, updated_at)), 'YYYY-MM') AS month, " +
                   "SUM(amount) FROM invoices WHERE clinic_id = :clinicId AND status = 'PAID' " +
                   "AND COALESCE(payment_date, updated_at) >= :from AND COALESCE(payment_date, updated_at) < :to " +
                   "GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> paidRevenueByMonth(@Param("clinicId") UUID clinicId,
                                      @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
