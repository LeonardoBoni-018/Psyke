package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.CancellationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CancellationLogRepository extends JpaRepository<CancellationLog, UUID> {

    List<CancellationLog> findByAppointmentId(UUID appointmentId);

    long countByCancelledByRoleAndCancelledAtBetween(
        String role, OffsetDateTime from, OffsetDateTime to);
}
