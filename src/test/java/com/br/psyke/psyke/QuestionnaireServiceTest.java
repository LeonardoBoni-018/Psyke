package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateQuestionnaireRequest;
import com.br.psyke.psyke.dto.UpdateQuestionnaireRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Questionnaire;
import com.br.psyke.psyke.repository.QuestionnaireRepository;
import com.br.psyke.psyke.service.QuestionnaireService;
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
class QuestionnaireServiceTest {

    @Mock private QuestionnaireRepository questionnaires;
    private QuestionnaireService service;
    private UUID clinicId;
    private Questionnaire existingQ;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireService(questionnaires);
        clinicId = UUID.randomUUID();
        existingQ = Questionnaire.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .title("GAD-7").description("Generalized Anxiety Disorder")
                .questions("[{\"question\":\"Feeling nervous?\"}]").active(true)
                .build();
    }

    @Test
    void shouldCreate() {
        var req = new CreateQuestionnaireRequest("PHQ-9", "Depression", "[{\"question\":\"Little interest?\"}]");
        var saved = Questionnaire.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .title("PHQ-9").questions("[{\"question\":\"Little interest?\"}]").active(true).build();

        when(questionnaires.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.title()).isEqualTo("PHQ-9");
    }

    @Test
    void shouldFindById() {
        when(questionnaires.findByIdAndClinicId(existingQ.getId(), clinicId))
                .thenReturn(Optional.of(existingQ));

        var resp = service.findById(existingQ.getId(), clinicId);
        assertThat(resp.title()).isEqualTo("GAD-7");
    }

    @Test
    void shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(questionnaires.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAll() {
        var pageable = PageRequest.of(0, 10);
        when(questionnaires.findByClinicId(clinicId, pageable))
                .thenReturn(new PageImpl<>(List.of(existingQ)));

        var resp = service.findAll(clinicId, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldUpdate() {
        var req = new UpdateQuestionnaireRequest("GAD-7 Updated", null, null, null);
        when(questionnaires.findByIdAndClinicId(existingQ.getId(), clinicId))
                .thenReturn(Optional.of(existingQ));
        when(questionnaires.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(existingQ.getId(), req, clinicId);
        assertThat(resp.title()).isEqualTo("GAD-7 Updated");
    }

    @Test
    void shouldDelete() {
        when(questionnaires.findByIdAndClinicId(existingQ.getId(), clinicId))
                .thenReturn(Optional.of(existingQ));

        service.delete(existingQ.getId(), clinicId);
        verify(questionnaires).delete(existingQ);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(questionnaires.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
