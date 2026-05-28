package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.professionalId = :pid " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED')")
    List<Appointment> findConflictingByProfessional(
        @Param("pid") UUID professionalId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.roomId = :roomId " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED')")
    List<Appointment> findConflictingByRoom(
        @Param("roomId") UUID roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    List<Appointment> findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
        UUID professionalId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByPatientIdAndStartTimeAfterOrderByStartTime(
        UUID patientId, LocalDateTime from);

    long countByProfessionalIdAndStartTimeBetweenAndStatusIn(
        UUID professionalId, LocalDateTime from, LocalDateTime to, List<Appointment.AppointmentStatus> statuses);
}
