package com.br.psyke.psyke;

import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.event.SessionEventProducer;
import com.br.psyke.psyke.exception.InvalidTransitionException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Patient;
import com.br.psyke.psyke.model.Professional;
import com.br.psyke.psyke.model.Slot;
import com.br.psyke.psyke.repository.AppointmentRepository;
import com.br.psyke.psyke.repository.RoomRepository;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.repository.ProfessionalRepository;
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.service.AppointmentService;
import com.br.psyke.psyke.service.CancellationLogService;
import com.br.psyke.psyke.service.SlotEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentDeletionTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private SlotRepository slotRepo;
    @Mock private ProfessionalRepository professionalRepo;
    @Mock private PatientRepository patientRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private AppointmentEventProducer eventProducer;
    @Mock private SessionEventProducer sessionEventProducer;
    @Mock private CancellationLogService cancellationLogService;
    @Mock private SlotEngine slotEngine;

    private AppointmentService service;
    private UUID appointmentId;
    private Appointment scheduledApp;
    private Appointment confirmedApp;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepo, slotRepo, professionalRepo, patientRepo, roomRepo,
                eventProducer, sessionEventProducer, cancellationLogService, slotEngine);
        appointmentId = UUID.randomUUID();
        var now = LocalDateTime.now().plusDays(1);
        scheduledApp = Appointment.builder().id(appointmentId)
                .clinicId(UUID.randomUUID()).patientId(UUID.randomUUID())
                .professionalId(UUID.randomUUID()).startTime(now).endTime(now.plusMinutes(50))
                .status(Appointment.AppointmentStatus.SCHEDULED).build();
        confirmedApp = Appointment.builder().id(UUID.randomUUID())
                .clinicId(UUID.randomUUID()).patientId(UUID.randomUUID())
                .professionalId(UUID.randomUUID()).startTime(now.plusHours(1)).endTime(now.plusHours(1).plusMinutes(50))
                .status(Appointment.AppointmentStatus.CONFIRMED).build();
    }

    @Test
    void shouldDeleteScheduledAppointment() {
        when(appointmentRepo.findById(appointmentId)).thenReturn(Optional.of(scheduledApp));
        when(slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                scheduledApp.getProfessionalId(), scheduledApp.getStartTime(), scheduledApp.getEndTime()))
                .thenReturn(List.of());

        service.delete(appointmentId);
        verify(appointmentRepo).delete(scheduledApp);
        verify(cancellationLogService).log(eq(appointmentId), eq(scheduledApp.getPatientId()), any(), any());
    }

    @Test
    void shouldFreeSlotsOnDelete() {
        var slot = Slot.builder().id(UUID.randomUUID()).professionalId(scheduledApp.getProfessionalId())
                .startTime(scheduledApp.getStartTime()).endTime(scheduledApp.getEndTime())
                .status(Slot.SlotStatus.BOOKED).patientId(scheduledApp.getPatientId()).build();

        when(appointmentRepo.findById(appointmentId)).thenReturn(Optional.of(scheduledApp));
        when(slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                scheduledApp.getProfessionalId(), scheduledApp.getStartTime(), scheduledApp.getEndTime()))
                .thenReturn(List.of(slot));

        service.delete(appointmentId);
        assertThat(slot.getStatus()).isEqualTo(Slot.SlotStatus.FREE);
        assertThat(slot.getPatientId()).isNull();
    }

    @Test
    void shouldThrowWhenDeletingConfirmedAppointment() {
        when(appointmentRepo.findById(confirmedApp.getId())).thenReturn(Optional.of(confirmedApp));

        assertThatThrownBy(() -> service.delete(confirmedApp.getId()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Only SCHEDULED");
    }

    @Test
    void shouldThrowWhenAppointmentNotFound() {
        when(appointmentRepo.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(appointmentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
