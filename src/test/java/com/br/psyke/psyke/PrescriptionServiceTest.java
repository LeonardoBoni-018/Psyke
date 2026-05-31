package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreatePrescriptionRequest;
import com.br.psyke.psyke.dto.UpdatePrescriptionRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Prescription;
import com.br.psyke.psyke.repository.PrescriptionRepository;
import com.br.psyke.psyke.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptions;
    private PrescriptionService service;
    private UUID clinicId;
    private Prescription existing;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptions);
        clinicId = UUID.randomUUID();
        existing = Prescription.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .medication("Sertralina 50mg").dosage("1cp/dia")
                .active(true).build();
    }

    @Test void shouldCreate() {
        var req = new CreatePrescriptionRequest(UUID.randomUUID(), UUID.randomUUID(), null,
                "Fluoxetina 20mg", "1cp/dia", "Tomar após café", null, null);
        var saved = Prescription.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(req.patientId()).professionalId(req.professionalId())
                .medication("Fluoxetina 20mg").dosage("1cp/dia").active(true).build();
        when(prescriptions.save(any())).thenReturn(saved);
        assertThat(service.create(req, clinicId).medication()).isEqualTo("Fluoxetina 20mg");
    }

    @Test void shouldFindById() {
        when(prescriptions.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        assertThat(service.findById(existing.getId(), clinicId).medication()).isEqualTo("Sertralina 50mg");
    }

    @Test void shouldThrowWhenNotFound() {
        when(prescriptions.findByIdAndClinicId(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(UUID.randomUUID(), clinicId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void shouldFindAll() {
        when(prescriptions.findByClinicId(clinicId, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(existing)));
        assertThat(service.findAll(clinicId, null, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test void shouldUpdate() {
        var req = new UpdatePrescriptionRequest("Paroxetina 20mg", null, null, null, null, null);
        when(prescriptions.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        when(prescriptions.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.update(existing.getId(), req, clinicId).medication()).isEqualTo("Paroxetina 20mg");
    }

    @Test void shouldDeactivate() {
        var req = new UpdatePrescriptionRequest(null, null, null, null, null, false);
        when(prescriptions.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        when(prescriptions.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.update(existing.getId(), req, clinicId).active()).isFalse();
    }

    @Test void shouldDelete() {
        when(prescriptions.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        service.delete(existing.getId(), clinicId);
        verify(prescriptions).delete(existing);
    }

    @Test void shouldThrowOnDeleteWhenNotFound() {
        when(prescriptions.findByIdAndClinicId(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), clinicId)).isInstanceOf(ResourceNotFoundException.class);
    }
}
