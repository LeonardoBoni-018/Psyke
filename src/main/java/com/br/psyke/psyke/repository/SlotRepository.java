package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Slot;
import com.br.psyke.psyke.model.Slot.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
        UUID professionalId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT s FROM Slot s WHERE s.professionalId = :pid AND s.startTime >= :from AND s.endTime <= :to AND s.status = :status ORDER BY s.startTime")
    List<Slot> findByProfessionalAndStatusInRange(
        @Param("pid") UUID professionalId, @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to, @Param("status") SlotStatus status);

    boolean existsByProfessionalIdAndStartTimeAndStatus(
        UUID professionalId, LocalDateTime startTime, SlotStatus status);
}
