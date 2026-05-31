package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateProfessionalRequest;
import com.br.psyke.psyke.dto.UpdateProfessionalRequest;
import com.br.psyke.psyke.exception.DuplicateResourceException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Professional;
import com.br.psyke.psyke.repository.ProfessionalRepository;
import com.br.psyke.psyke.service.ProfessionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @Mock private ProfessionalRepository professionals;
    private ProfessionalService service;
    private UUID clinicId;
    private Professional existingProfessional;

    @BeforeEach
    void setUp() {
        service = new ProfessionalService(professionals);
        clinicId = UUID.randomUUID();
        existingProfessional = Professional.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .crp("06/12345").fullName("Dra. Ana")
                .specialty("Psicologia Clínica").approach("TCC")
                .sessionValue(new BigDecimal("250.00")).sessionDuration(50)
                .acceptsInsurance(true).active(true)
                .build();
    }

    @Test
    void shouldCreateProfessional() {
        var req = new CreateProfessionalRequest("06/54321", "Dr. Carlos",
                "Psicanálise", "Freudiana", "Experiência em clínica",
                new BigDecimal("300.00"), 60, true);

        var saved = Professional.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .crp("06/54321").fullName("Dr. Carlos")
                .specialty("Psicanálise").approach("Freudiana")
                .resume("Experiência em clínica")
                .sessionValue(new BigDecimal("300.00")).sessionDuration(60)
                .acceptsInsurance(true).active(true).build();

        when(professionals.existsByClinicIdAndCrp(clinicId, "06/54321")).thenReturn(false);
        when(professionals.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.fullName()).isEqualTo("Dr. Carlos");
        assertThat(resp.crp()).isEqualTo("06/54321");
        assertThat(resp.active()).isTrue();
    }

    @Test
    void shouldThrowWhenCrpAlreadyExists() {
        var req = new CreateProfessionalRequest("06/12345", "Outro",
                null, null, null, null, null, null);

        when(professionals.existsByClinicIdAndCrp(clinicId, "06/12345")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, clinicId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CRP");
    }

    @Test
    void shouldFindProfessionalById() {
        when(professionals.findByIdAndClinicId(existingProfessional.getId(), clinicId))
                .thenReturn(Optional.of(existingProfessional));

        var resp = service.findById(existingProfessional.getId(), clinicId);
        assertThat(resp.fullName()).isEqualTo("Dra. Ana");
    }

    @Test
    void shouldThrowWhenProfessionalNotFound() {
        var id = UUID.randomUUID();
        when(professionals.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllProfessionalsWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingProfessional));

        when(professionals.findByClinicId(clinicId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, pageable);
        assertThat(resp).hasSize(1);
        assertThat(resp.getContent().get(0).fullName()).isEqualTo("Dra. Ana");
    }

    @Test
    void shouldUpdateProfessional() {
        var id = existingProfessional.getId();
        var req = new UpdateProfessionalRequest(null, "Dra. Ana Updated",
                null, null, null, new BigDecimal("300.00"), null, null, null);

        when(professionals.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingProfessional));
        when(professionals.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, clinicId);
        assertThat(resp.fullName()).isEqualTo("Dra. Ana Updated");
        assertThat(resp.sessionValue()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void shouldThrowOnUpdateWhenCrpConflict() {
        var id = existingProfessional.getId();
        var req = new UpdateProfessionalRequest("06/99999", null,
                null, null, null, null, null, null, null);

        when(professionals.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingProfessional));
        when(professionals.existsByClinicIdAndCrpAndIdNot(clinicId, "06/99999", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, req, clinicId))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void shouldDeleteProfessional() {
        var id = existingProfessional.getId();
        when(professionals.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingProfessional));

        service.delete(id, clinicId);
        verify(professionals).delete(existingProfessional);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(professionals.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeactivateInsteadOfDelete() {
        var id = existingProfessional.getId();
        var req = new UpdateProfessionalRequest(null, null,
                null, null, null, null, null, null, false);

        when(professionals.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingProfessional));
        when(professionals.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, clinicId);
        assertThat(resp.active()).isFalse();
    }
}
