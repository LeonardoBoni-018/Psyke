package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {

    List<AvailabilityException> findByProfessionalIdAndActiveTrue(UUID professionalId);

    @Query("SELECT e FROM AvailabilityException e WHERE e.professionalId = :pid AND e.active = true AND e.exceptionDate BETWEEN :from AND :to")
    List<AvailabilityException> findActiveInRange(
        @Param("pid") UUID professionalId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);
}
