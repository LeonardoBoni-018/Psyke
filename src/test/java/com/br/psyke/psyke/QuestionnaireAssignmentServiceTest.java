package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.SubmitAnswersRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.QuestionnaireAssignment;
import com.br.psyke.psyke.repository.QuestionnaireAssignmentRepository;
import com.br.psyke.psyke.service.QuestionnaireAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
class QuestionnaireAssignmentServiceTest {

    @Mock private QuestionnaireAssignmentRepository assignments;
    private QuestionnaireAssignmentService service;
    private UUID clinicId;
    private QuestionnaireAssignment existing;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireAssignmentService(assignments);
        clinicId = UUID.randomUUID();
        existing = QuestionnaireAssignment.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .questionnaireId(UUID.randomUUID()).patientId(UUID.randomUUID())
                .status(QuestionnaireAssignment.AssignmentStatus.PENDING)
                .build();
    }

    @Test
    void shouldAssign() {
        when(assignments.save(any())).thenReturn(existing);

        var resp = service.assign(clinicId, UUID.randomUUID(), UUID.randomUUID(), null);
        assertThat(resp.status()).isEqualTo("PENDING");
    }

    @Test
    void shouldSubmit() {
        var id = existing.getId();
        var req = new SubmitAnswersRequest("[{\"answer\":2}]", new BigDecimal("6"));

        when(assignments.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existing));
        when(assignments.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.submit(id, clinicId, req);
        assertThat(resp.status()).isEqualTo("COMPLETED");
        assertThat(resp.score()).isEqualByComparingTo(new BigDecimal("6"));
    }

    @Test
    void shouldFindById() {
        when(assignments.findByIdAndClinicId(existing.getId(), clinicId))
                .thenReturn(Optional.of(existing));

        var resp = service.findById(existing.getId(), clinicId);
        assertThat(resp.status()).isEqualTo("PENDING");
    }

    @Test
    void shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(assignments.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAll() {
        var pageable = PageRequest.of(0, 10);
        when(assignments.findByClinicId(clinicId, pageable))
                .thenReturn(new PageImpl<>(List.of(existing)));

        var resp = service.findAll(clinicId, null, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldDelete() {
        when(assignments.findByIdAndClinicId(existing.getId(), clinicId))
                .thenReturn(Optional.of(existing));

        service.delete(existing.getId(), clinicId);
        verify(assignments).delete(existing);
    }
}
