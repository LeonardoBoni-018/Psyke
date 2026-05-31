package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.AvailabilityRequest;
import com.br.psyke.psyke.dto.ExceptionRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.AvailabilityException;
import com.br.psyke.psyke.model.ProfessionalAvailability;
import com.br.psyke.psyke.repository.AvailabilityExceptionRepository;
import com.br.psyke.psyke.repository.ProfessionalAvailabilityRepository;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.service.ProfessionalAvailabilityService;
import com.br.psyke.psyke.service.SlotEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalAvailabilityServiceTest {

    @Mock private ProfessionalAvailabilityRepository availabilityRepo;
    @Mock private AvailabilityExceptionRepository exceptionRepo;
    @Mock private SlotRepository slotRepo;
    @Mock private SlotEngine slotEngine;
    private ProfessionalAvailabilityService service;
    private UUID professionalId;
    private ProfessionalAvailability weeklyAvail;
    private AvailabilityException exception;

    @BeforeEach
    void setUp() {
        service = new ProfessionalAvailabilityService(availabilityRepo, exceptionRepo, slotRepo, slotEngine);
        professionalId = UUID.randomUUID();
        weeklyAvail = ProfessionalAvailability.builder()
                .id(UUID.randomUUID()).professionalId(professionalId)
                .dayOfWeek(2).startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0)).sessionDuration(50).active(true)
                .build();
        exception = AvailabilityException.builder()
                .id(UUID.randomUUID()).professionalId(professionalId)
                .exceptionDate(LocalDate.of(2026, 6, 5))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(11, 0))
                .type(AvailabilityException.ExceptionType.HOLIDAY)
                .reason("Feriado").active(true)
                .build();
    }

    @Test
    void shouldCreateAvailability() {
        var req = new AvailabilityRequest(2, LocalTime.of(8, 0), LocalTime.of(12, 0), 50);
        when(availabilityRepo.save(any())).thenReturn(weeklyAvail);

        var resp = service.createAvailability(professionalId, req);
        assertThat(resp.dayOfWeek()).isEqualTo(2);
        assertThat(resp.sessionDuration()).isEqualTo(50);
    }

    @Test
    void shouldListAvailability() {
        when(availabilityRepo.findByProfessionalIdAndActiveTrueOrderByDayOfWeek(professionalId))
                .thenReturn(List.of(weeklyAvail));

        var resp = service.listAvailability(professionalId);
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).dayOfWeek()).isEqualTo(2);
    }

    @Test
    void shouldUpdateAvailability() {
        var req = new AvailabilityRequest(3, LocalTime.of(9, 0), LocalTime.of(13, 0), 60);
        when(availabilityRepo.findById(weeklyAvail.getId())).thenReturn(Optional.of(weeklyAvail));
        when(availabilityRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.updateAvailability(weeklyAvail.getId(), req);
        assertThat(resp.dayOfWeek()).isEqualTo(3);
        assertThat(resp.sessionDuration()).isEqualTo(60);
    }

    @Test
    void shouldThrowOnUpdateAvailabilityWhenNotFound() {
        var id = UUID.randomUUID();
        var req = new AvailabilityRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0), 50);

        when(availabilityRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAvailability(id, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteAvailability() {
        service.deleteAvailability(weeklyAvail.getId());
        verify(availabilityRepo).deleteById(weeklyAvail.getId());
    }

    @Test
    void shouldCreateException() {
        var req = new ExceptionRequest(LocalDate.of(2026, 6, 5),
                LocalTime.of(9, 0), LocalTime.of(11, 0), "HOLIDAY", "Feriado");
        when(exceptionRepo.save(any())).thenReturn(exception);

        var resp = service.createException(professionalId, req);
        assertThat(resp.type()).isEqualTo("HOLIDAY");
    }

    @Test
    void shouldListExceptions() {
        when(exceptionRepo.findByProfessionalIdAndActiveTrue(professionalId))
                .thenReturn(List.of(exception));

        var resp = service.listExceptions(professionalId);
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).type()).isEqualTo("HOLIDAY");
    }

    @Test
    void shouldSoftDeleteException() {
        when(exceptionRepo.findById(exception.getId())).thenReturn(Optional.of(exception));

        service.deleteException(exception.getId());
        assertThat(exception.isActive()).isFalse();
        verify(exceptionRepo).save(exception);
    }

    @Test
    void shouldThrowOnDeleteExceptionWhenNotFound() {
        var id = UUID.randomUUID();
        when(exceptionRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteException(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetFreeSlots() {
        var from = LocalDate.of(2026, 6, 1);
        var to = LocalDate.of(2026, 6, 7);
        when(slotEngine.generateFreeSlots(professionalId, from, to, 50))
                .thenReturn(List.of());

        var resp = service.getSlots(professionalId, from, to, 50);
        assertThat(resp).isEmpty();
    }
}
