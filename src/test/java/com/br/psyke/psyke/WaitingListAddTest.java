package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.WaitingListRequest;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.model.WaitingList;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.repository.WaitingListRepository;
import com.br.psyke.psyke.service.WaitingListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingListAddTest {

    @Mock private WaitingListRepository repo;
    @Mock private PatientRepository patientRepo;
    @Mock private SlotRepository slotRepo;
    @Mock private AppointmentEventProducer eventProducer;

    private WaitingListService service;
    private UUID patientId;
    private UUID professionalId;

    @BeforeEach
    void setUp() {
        service = new WaitingListService(repo, patientRepo, slotRepo, eventProducer);
        patientId = UUID.randomUUID();
        professionalId = UUID.randomUUID();
    }

    @Test
    void shouldAddToWaitingList() {
        var patient = com.br.psyke.psyke.model.Patient.builder().id(patientId).fullName("Patient").build();
        var now = OffsetDateTime.now();
        var saved = WaitingList.builder().id(UUID.randomUUID())
                .patientId(patientId).professionalId(professionalId)
                .priority(0).status(WaitingList.WaitingStatus.WAITING)
                .requestedAt(now).build();

        when(patientRepo.findById(patientId)).thenReturn(Optional.of(patient));
        when(repo.findByPatientIdAndProfessionalIdAndStatus(patientId, professionalId, WaitingList.WaitingStatus.WAITING))
                .thenReturn(Optional.empty());
        when(repo.save(any())).thenReturn(saved);

        var req = new WaitingListRequest(patientId, professionalId, 0);
        var result = service.join(req);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("WAITING");
        verify(repo).save(any(WaitingList.class));
    }

    @Test
    void shouldThrowWhenPatientNotFound() {
        when(patientRepo.findById(patientId)).thenReturn(Optional.empty());

        var req = new WaitingListRequest(patientId, professionalId, 0);
        assertThatThrownBy(() -> service.join(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void shouldThrowWhenPatientAlreadyInList() {
        var patient = com.br.psyke.psyke.model.Patient.builder().id(patientId).fullName("Patient").build();
        var existing = WaitingList.builder().id(UUID.randomUUID())
                .patientId(patientId).professionalId(professionalId)
                .status(WaitingList.WaitingStatus.WAITING).build();

        when(patientRepo.findById(patientId)).thenReturn(Optional.of(patient));
        when(repo.findByPatientIdAndProfessionalIdAndStatus(patientId, professionalId, WaitingList.WaitingStatus.WAITING))
                .thenReturn(Optional.of(existing));

        var req = new WaitingListRequest(patientId, professionalId, 0);
        assertThatThrownBy(() -> service.join(req))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("already in waiting list");
    }
}
