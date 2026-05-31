package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.AppointmentRequest;
import com.br.psyke.psyke.dto.UpdateAppointmentRequest;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.event.SessionEventProducer;
import com.br.psyke.psyke.exception.InvalidTransitionException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Room;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.repository.AppointmentRepository;
import com.br.psyke.psyke.repository.RoomRepository;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private SlotRepository slotRepo;
    @Mock private UserRepository userRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private AppointmentEventProducer eventProducer;
    @Mock private SessionEventProducer sessionEventProducer;
    @Mock private CancellationLogService cancellationLogService;
    @Mock private SlotEngine slotEngine;
    private AppointmentService service;
    private Appointment existingAppointment;
    private User professional;
    private User patient;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepo, slotRepo, userRepo, roomRepo,
                eventProducer, sessionEventProducer, cancellationLogService, slotEngine);
        var clinicId = UUID.randomUUID();
        professional = User.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .fullName("Dr. Ana").email("ana@test.com").active(true).build();
        patient = User.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .fullName("João").email("joao@test.com").active(true).build();
        room = Room.builder().id(UUID.randomUUID()).name("Sala 01").active(true).build();
        existingAppointment = Appointment.builder()
                .id(UUID.randomUUID()).clinicId(clinicId).patientId(patient.getId())
                .professionalId(professional.getId()).roomId(room.getId())
                .startTime(LocalDateTime.of(2026, 6, 1, 9, 0))
                .endTime(LocalDateTime.of(2026, 6, 1, 9, 50))
                .status(Appointment.AppointmentStatus.SCHEDULED)
                .notes("Initial consultation")
                .build();
        var now = LocalDateTime.now();
        existingAppointment.setCreatedAt(now);
        existingAppointment.setUpdatedAt(now);
    }

    @Test
    void shouldFindById() {
        when(appointmentRepo.findById(existingAppointment.getId()))
                .thenReturn(Optional.of(existingAppointment));

        var resp = service.findById(existingAppointment.getId());
        assertThat(resp.status()).isEqualTo("SCHEDULED");
    }

    @Test
    void shouldThrowOnFindByIdWhenNotFound() {
        var id = UUID.randomUUID();
        when(appointmentRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateNotes() {
        var id = existingAppointment.getId();
        var req = new UpdateAppointmentRequest(null, "Updated notes");

        when(appointmentRepo.findById(id)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req);
        assertThat(resp.notes()).isEqualTo("Updated notes");
    }

    @Test
    void shouldUpdateRoom() {
        var id = existingAppointment.getId();
        var newRoomId = UUID.randomUUID();
        var req = new UpdateAppointmentRequest(newRoomId, null);

        when(appointmentRepo.findById(id)).thenReturn(Optional.of(existingAppointment));
        when(roomRepo.findById(newRoomId)).thenReturn(Optional.of(
                Room.builder().id(newRoomId).name("Sala 02").active(true).build()));
        when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req);
        assertThat(resp.roomId()).isEqualTo(newRoomId);
    }

    @Test
    void shouldThrowOnUpdateWhenAppointmentNotFound() {
        var id = UUID.randomUUID();
        when(appointmentRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateAppointmentRequest(null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowOnUpdateWhenRoomNotFound() {
        var id = existingAppointment.getId();
        var newRoomId = UUID.randomUUID();
        var req = new UpdateAppointmentRequest(newRoomId, null);

        when(appointmentRepo.findById(id)).thenReturn(Optional.of(existingAppointment));
        when(roomRepo.findById(newRoomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllWithFilters() {
        when(appointmentRepo.findAllByFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of(existingAppointment));

        var resp = service.findAll(null, null, null, null, null);
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).status()).isEqualTo("SCHEDULED");
    }

    @Test
    void shouldDeleteScheduledAppointment() {
        when(appointmentRepo.findById(existingAppointment.getId()))
                .thenReturn(Optional.of(existingAppointment));

        service.delete(existingAppointment.getId());
        verify(appointmentRepo).delete(existingAppointment);
    }

    @Test
    void shouldThrowOnDeleteWhenNotScheduled() {
        existingAppointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        when(appointmentRepo.findById(existingAppointment.getId()))
                .thenReturn(Optional.of(existingAppointment));

        assertThatThrownBy(() -> service.delete(existingAppointment.getId()))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(appointmentRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
