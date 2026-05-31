package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateTreatmentPlanRequest;
import com.br.psyke.psyke.dto.UpdateTreatmentPlanRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.TreatmentPlan;
import com.br.psyke.psyke.repository.TreatmentPlanRepository;
import com.br.psyke.psyke.service.TreatmentPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    @Mock private TreatmentPlanRepository plans;
    private TreatmentPlanService service;
    private UUID clinicId;
    private TreatmentPlan existingPlan;

    @BeforeEach
    void setUp() {
        service = new TreatmentPlanService(plans);
        clinicId = UUID.randomUUID();
        existingPlan = TreatmentPlan.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .prontuarioId(UUID.randomUUID())
                .title("Plano para Ansiedade")
                .frequency("weekly").estimatedSessions(20)
                .status(TreatmentPlan.PlanStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldCreatePlan() {
        var req = new CreateTreatmentPlanRequest(UUID.randomUUID(), "Plano TCC",
                "Descrição", "Metas", "Técnicas", "weekly", 12,
                LocalDate.of(2026, 6, 1), null);
        var saved = TreatmentPlan.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .prontuarioId(req.prontuarioId()).title("Plano TCC")
                .description("Descrição").goals("Metas").techniques("Técnicas")
                .frequency("weekly").estimatedSessions(12)
                .status(TreatmentPlan.PlanStatus.ACTIVE).build();

        when(plans.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.title()).isEqualTo("Plano TCC");
        assertThat(resp.frequency()).isEqualTo("weekly");
    }

    @Test
    void shouldFindById() {
        when(plans.findByIdAndClinicId(existingPlan.getId(), clinicId))
                .thenReturn(Optional.of(existingPlan));

        var resp = service.findById(existingPlan.getId(), clinicId);
        assertThat(resp.title()).isEqualTo("Plano para Ansiedade");
    }

    @Test
    void shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(plans.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingPlan));

        when(plans.findByClinicId(clinicId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, null, pageable);
        assertThat(resp).hasSize(1);
        assertThat(resp.getContent().get(0).title()).isEqualTo("Plano para Ansiedade");
    }

    @Test
    void shouldFindByProntuarioId() {
        var prontuarioId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingPlan));

        when(plans.findByClinicIdAndProntuarioId(clinicId, prontuarioId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, prontuarioId, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldUpdatePlan() {
        var id = existingPlan.getId();
        var req = new UpdateTreatmentPlanRequest("Plano Atualizado", null, null,
                null, null, null, "COMPLETED", null, null);

        when(plans.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingPlan));
        when(plans.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, clinicId);
        assertThat(resp.title()).isEqualTo("Plano Atualizado");
        assertThat(resp.status()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldThrowOnUpdateWhenNotFound() {
        var id = UUID.randomUUID();
        when(plans.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateTreatmentPlanRequest(null, null, null, null, null, null, null, null, null), clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeletePlan() {
        var id = existingPlan.getId();
        when(plans.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingPlan));

        service.delete(id, clinicId);
        verify(plans).delete(existingPlan);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(plans.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
