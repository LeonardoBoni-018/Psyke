package com.br.psyke.psyke;

import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
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
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingListServiceTest {

    @Mock private WaitingListRepository repo;
    @Mock private PatientRepository patientRepo;
    @Mock private SlotRepository slotRepo;
    @Mock private AppointmentEventProducer eventProducer;

    private WaitingListService service;
    private WaitingList waitingEntry;
    private WaitingList offeredEntry;

    @BeforeEach
    void setUp() {
        service = new WaitingListService(repo, patientRepo, slotRepo, eventProducer);
        waitingEntry = WaitingList.builder().id(UUID.randomUUID())
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .priority(0).status(WaitingList.WaitingStatus.WAITING).build();
        offeredEntry = WaitingList.builder().id(UUID.randomUUID())
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .priority(0).status(WaitingList.WaitingStatus.OFFERED).build();
    }

    @Test
    void shouldRemoveWaitingEntry() {
        when(repo.findById(waitingEntry.getId())).thenReturn(Optional.of(waitingEntry));

        service.remove(waitingEntry.getId());
        verify(repo).delete(waitingEntry);
    }

    @Test
    void shouldThrowWhenRemovingOfferedEntry() {
        when(repo.findById(offeredEntry.getId())).thenReturn(Optional.of(offeredEntry));

        assertThatThrownBy(() -> service.remove(offeredEntry.getId()))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("Only WAITING");
    }

    @Test
    void shouldThrowWhenEntryNotFound() {
        var id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
