package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.AppointmentRequest;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.service.AppointmentService;
import com.br.psyke.psyke.service.CancellationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentConflictTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private SlotRepository slotRepo;
    @Mock private UserRepository userRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private AppointmentEventProducer eventProducer;
    @Mock private CancellationLogService cancellationLogService;
    @Captor private ArgumentCaptor<Appointment> appointmentCaptor;

    private AppointmentService service;
    private PasswordEncoder encoder;
    private User professional;
    private User patient;
    private Room room;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new AppointmentService(appointmentRepo, slotRepo, userRepo, roomRepo, eventProducer, cancellationLogService);

        var clinicId = UUID.randomUUID();
        professional = User.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .fullName("Prof").email("prof@test.com").passwordHash(encoder.encode("pass"))
                .active(true).build();
        patient = User.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .fullName("Patient").email("pat@test.com").passwordHash(encoder.encode("pass"))
                .active(true).build();
        room = Room.builder().id(UUID.randomUUID()).clinicId(clinicId).name("Room 1").active(true).build();
        now = LocalDateTime.now();
    }

    private Appointment savedAppointment(AppointmentRequest req, Appointment.AppointmentStatus status) {
        return Appointment.builder().id(UUID.randomUUID()).clinicId(professional.getClinicId())
                .patientId(req.patientId()).professionalId(req.professionalId()).roomId(req.roomId())
                .startTime(req.startTime()).endTime(req.endTime()).status(status)
                .notes(req.notes()).createdAt(now).updatedAt(now).build();
    }

    @Test
    void shouldCreateAppointmentWhenSlotFree() {
        var start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        var end = start.plusMinutes(50);
        var req = new AppointmentRequest(patient.getId(), professional.getId(), room.getId(), start, end, "Test");
        var saved = savedAppointment(req, Appointment.AppointmentStatus.SCHEDULED);

        when(userRepo.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(userRepo.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(slotRepo.existsByProfessionalIdAndStartTimeAndStatus(professional.getId(), start, Slot.SlotStatus.FREE))
                .thenReturn(true);
        when(appointmentRepo.findConflictingByProfessional(professional.getId(), start, end))
                .thenReturn(List.of());
        when(appointmentRepo.findConflictingByRoom(room.getId(), start, end))
                .thenReturn(List.of());
        when(appointmentRepo.save(any())).thenReturn(saved);

        var resp = service.create(req);
        assertThat(resp.status()).isEqualTo("SCHEDULED");
        verify(eventProducer).publish(any());
    }

    @Test
    void shouldThrowConflictWhenSlotAlreadyBooked() {
        var start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        var end = start.plusMinutes(50);

        when(userRepo.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(userRepo.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(slotRepo.existsByProfessionalIdAndStartTimeAndStatus(professional.getId(), start, Slot.SlotStatus.FREE))
                .thenReturn(false);

        var req = new AppointmentRequest(patient.getId(), professional.getId(), room.getId(), start, end, "Conflict");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("Slot not available");
    }

    @Test
    void shouldThrowConflictWhenProfessionalDoubleBooked() {
        var start = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
        var end = start.plusMinutes(50);
        var existing = Appointment.builder().id(UUID.randomUUID()).professionalId(professional.getId())
                .startTime(start).endTime(end).status(Appointment.AppointmentStatus.SCHEDULED).build();

        when(userRepo.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(userRepo.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(slotRepo.existsByProfessionalIdAndStartTimeAndStatus(professional.getId(), start, Slot.SlotStatus.FREE))
                .thenReturn(true);
        when(appointmentRepo.findConflictingByProfessional(professional.getId(), start, end))
                .thenReturn(List.of(existing));

        var req = new AppointmentRequest(patient.getId(), professional.getId(), room.getId(), start, end, "Conflict");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("Professional already has an appointment");
    }

    @Test
    void shouldThrowConflictWhenRoomDoubleBooked() {
        var start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        var end = start.plusMinutes(50);
        var existing = Appointment.builder().id(UUID.randomUUID()).roomId(room.getId())
                .startTime(start).endTime(end).status(Appointment.AppointmentStatus.CONFIRMED).build();

        when(userRepo.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(userRepo.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(slotRepo.existsByProfessionalIdAndStartTimeAndStatus(professional.getId(), start, Slot.SlotStatus.FREE))
                .thenReturn(true);
        when(appointmentRepo.findConflictingByProfessional(professional.getId(), start, end))
                .thenReturn(List.of());
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(appointmentRepo.findConflictingByRoom(room.getId(), start, end))
                .thenReturn(List.of(existing));

        var req = new AppointmentRequest(patient.getId(), professional.getId(), room.getId(), start, end, "Conflict");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("Room is already booked");
    }

    @Test
    void shouldThrowNotFoundWhenProfessionalMissing() {
        var start = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0);
        var end = start.plusMinutes(50);

        when(userRepo.findById(professional.getId())).thenReturn(Optional.empty());

        var req = new AppointmentRequest(patient.getId(), professional.getId(), room.getId(), start, end, "No prof");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Professional not found");
    }

    @Test
    void shouldPublishEventOnSuccessfulCreation() {
        var start = LocalDateTime.now().plusDays(1).withHour(16).withMinute(0);
        var end = start.plusMinutes(50);
        var req = new AppointmentRequest(patient.getId(), professional.getId(), room.getId(), start, end, "Pub");
        var saved = savedAppointment(req, Appointment.AppointmentStatus.SCHEDULED);

        when(userRepo.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(userRepo.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(slotRepo.existsByProfessionalIdAndStartTimeAndStatus(professional.getId(), start, Slot.SlotStatus.FREE))
                .thenReturn(true);
        when(appointmentRepo.findConflictingByProfessional(professional.getId(), start, end))
                .thenReturn(List.of());
        when(appointmentRepo.findConflictingByRoom(room.getId(), start, end))
                .thenReturn(List.of());
        when(appointmentRepo.save(any())).thenReturn(saved);

        service.create(req);
        verify(eventProducer).publish(argThat(e -> e.eventType().equals("scheduled")));
    }

    @Test
    void shouldNotRequireRoomForAppointment() {
        var start = LocalDateTime.now().plusDays(1).withHour(17).withMinute(0);
        var end = start.plusMinutes(50);
        var req = new AppointmentRequest(patient.getId(), professional.getId(), null, start, end, "No room");
        var saved = savedAppointment(req, Appointment.AppointmentStatus.SCHEDULED);

        when(userRepo.findById(professional.getId())).thenReturn(Optional.of(professional));
        when(userRepo.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(slotRepo.existsByProfessionalIdAndStartTimeAndStatus(professional.getId(), start, Slot.SlotStatus.FREE))
                .thenReturn(true);
        when(appointmentRepo.findConflictingByProfessional(professional.getId(), start, end))
                .thenReturn(List.of());
        when(appointmentRepo.save(any())).thenReturn(saved);

        var resp = service.create(req);
        assertThat(resp.status()).isEqualTo("SCHEDULED");
        verify(appointmentRepo, never()).findConflictingByRoom(any(), any(), any());
    }
}
