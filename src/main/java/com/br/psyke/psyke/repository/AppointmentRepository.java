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

    @Query("SELECT a.status, COUNT(a) FROM Appointment a WHERE a.clinicId = :clinicId AND a.startTime >= :from AND a.startTime < :to GROUP BY a.status")
    List<Object[]> countByClinicIdAndStartTimeBetweenGroupByStatus(
        @Param("clinicId") UUID clinicId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    /** Rows of [professionalId, count] of completed sessions in [from, to). */
    @Query("SELECT a.professionalId, COUNT(a) FROM Appointment a WHERE a.clinicId = :clinicId AND a.status = 'DONE' " +
           "AND a.startTime >= :from AND a.startTime < :to GROUP BY a.professionalId ORDER BY COUNT(a) DESC")
    List<Object[]> countDoneByProfessional(
        @Param("clinicId") UUID clinicId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE a.clinicId = :clinicId AND a.startTime >= :from AND a.startTime < :to " +
           "ORDER BY a.startTime")
    List<Appointment> findForExport(
        @Param("clinicId") UUID clinicId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    long countByProfessionalIdAndStartTimeBetweenAndStatusIn(
        UUID professionalId, LocalDateTime from, LocalDateTime to, List<Appointment.AppointmentStatus> statuses);

    @Query(value = "SELECT * FROM appointments a WHERE " +
           "(CAST(:professionalId AS uuid) IS NULL OR a.professional_id = CAST(:professionalId AS uuid)) AND " +
           "(CAST(:patientId AS uuid) IS NULL OR a.patient_id = CAST(:patientId AS uuid)) AND " +
           "(CAST(:status AS varchar) IS NULL OR a.status = CAST(:status AS varchar)) AND " +
           "(CAST(:from AS timestamp) IS NULL OR a.start_time >= CAST(:from AS timestamp)) AND " +
           "(CAST(:to AS timestamp) IS NULL OR a.end_time <= CAST(:to AS timestamp)) " +
           "ORDER BY a.start_time", nativeQuery = true)
    List<Appointment> findAllByFilters(
        @Param("professionalId") UUID professionalId,
        @Param("patientId") UUID patientId,
        @Param("status") Appointment.AppointmentStatus status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
}
