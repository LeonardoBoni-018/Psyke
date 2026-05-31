package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateConsentTermRequest;
import com.br.psyke.psyke.dto.UpdateConsentTermRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.ConsentTerm;
import com.br.psyke.psyke.repository.ConsentTermRepository;
import com.br.psyke.psyke.service.ConsentTermService;
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
class ConsentTermServiceTest {

    @Mock private ConsentTermRepository terms;
    private ConsentTermService service;
    private UUID clinicId;
    private ConsentTerm existing;

    @BeforeEach
    void setUp() {
        service = new ConsentTermService(terms);
        clinicId = UUID.randomUUID();
        existing = ConsentTerm.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .title("Termo de Consentimento").content("Texto completo...")
                .active(true).build();
    }

    @Test void shouldCreate() {
        var req = new CreateConsentTermRequest(UUID.randomUUID(), UUID.randomUUID(), "Termo", "Conteúdo", null);
        when(terms.save(any())).thenReturn(existing);
        assertThat(service.create(req, clinicId).title()).isEqualTo("Termo de Consentimento");
    }

    @Test void shouldFindById() {
        when(terms.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        assertThat(service.findById(existing.getId(), clinicId).title()).isEqualTo("Termo de Consentimento");
    }

    @Test void shouldThrowWhenNotFound() {
        when(terms.findByIdAndClinicId(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(UUID.randomUUID(), clinicId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void shouldFindAll() {
        when(terms.findByClinicId(clinicId, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(existing)));
        assertThat(service.findAll(clinicId, null, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test void shouldSignAsPatient() {
        when(terms.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        when(terms.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.sign(existing.getId(), clinicId, false).signedByPatient()).isTrue();
    }

    @Test void shouldSignAsProfessional() {
        when(terms.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        when(terms.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.sign(existing.getId(), clinicId, true).signedByProfessional()).isTrue();
    }

    @Test void shouldUpdate() {
        var req = new UpdateConsentTermRequest("Termo Atualizado", null, null, null, null);
        when(terms.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        when(terms.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.update(existing.getId(), req, clinicId).title()).isEqualTo("Termo Atualizado");
    }

    @Test void shouldDelete() {
        when(terms.findByIdAndClinicId(existing.getId(), clinicId)).thenReturn(Optional.of(existing));
        service.delete(existing.getId(), clinicId);
        verify(terms).delete(existing);
    }
}
