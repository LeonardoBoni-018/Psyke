package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.prontuario.*;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Anamnese;
import com.br.psyke.psyke.model.EvolucaoClinica;
import com.br.psyke.psyke.model.Prontuario;
import com.br.psyke.psyke.model.User;
import com.br.psyke.psyke.repository.AnamneseRepository;
import com.br.psyke.psyke.repository.EvolucaoClinicaRepository;
import com.br.psyke.psyke.repository.ProntuarioRepository;
import com.br.psyke.psyke.repository.UserRepository;
import com.br.psyke.psyke.service.prontuario.ProntuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProntuarioServiceTest {

    @Mock private ProntuarioRepository prontuarioRepo;
    @Mock private EvolucaoClinicaRepository evolucaoRepo;
    @Mock private AnamneseRepository anamneseRepo;
    @Mock private UserRepository userRepo;

    private ProntuarioService service;
    private UUID professionalId;
    private UUID patientId;
    private UUID otherUserId;
    private Prontuario prontuario;
    private EvolucaoClinica evolucao;
    private Anamnese anamnese;
    private User patient;

    @BeforeEach
    void setUp() {
        service = new ProntuarioService(prontuarioRepo, evolucaoRepo, anamneseRepo, userRepo);
        professionalId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        patient = User.builder().id(patientId).fullName("Patient").build();

        prontuario = Prontuario.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .professionalId(professionalId)
            .status(Prontuario.ProntuarioStatus.ACTIVE)
            .allergies("Pólen")
            .chronicConditions("Asma")
            .medications("Salbutamol")
            .notes("Paciente em acompanhamento")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        evolucao = EvolucaoClinica.builder()
            .id(UUID.randomUUID())
            .prontuarioId(prontuario.getId())
            .appointmentId(UUID.randomUUID())
            .professionalId(professionalId)
            .subjective("Paciente relata melhora")
            .objective("Comunicativo, orientado")
            .assessment("Ansiedade reduzida")
            .plan("Manter terapia semanal")
            .techniques("TCC")
            .createdAt(OffsetDateTime.now())
            .build();

        anamnese = Anamnese.builder()
            .id(UUID.randomUUID())
            .prontuarioId(prontuario.getId())
            .professionalId(professionalId)
            .chiefComplaint("Ansiedade")
            .historyIllness("Início há 6 meses")
            .personalHistory("Sem comorbidades")
            .familyHistory("Mãe com ansiedade")
            .socialHistory("Trabalha home office")
            .medications("Escitalopram")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    // --- create ---

    @Test
    void shouldCreateProntuario() {
        var req = new ProntuarioRequest(patientId, "Pólen", "Asma", "Salbutamol", "Acompanhamento");

        when(userRepo.findById(patientId)).thenReturn(Optional.of(patient));
        when(prontuarioRepo.existsByPatientId(patientId)).thenReturn(false);
        when(prontuarioRepo.save(any())).thenReturn(prontuario);

        var resp = service.create(req, professionalId, false);

        assertThat(resp.patientId()).isEqualTo(patientId);
        assertThat(resp.allergies()).isEqualTo("Pólen");
        assertThat(resp.status()).isEqualTo("ACTIVE");
        verify(prontuarioRepo).save(any());
    }

    @Test
    void shouldThrowWhenPatientAlreadyHasProntuario() {
        var req = new ProntuarioRequest(patientId, null, null, null, null);

        when(prontuarioRepo.existsByPatientId(patientId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, professionalId, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a prontuário");
        verify(prontuarioRepo, never()).save(any());
    }

    @Test
    void shouldThrowWhenPatientNotFoundForNonAdmin() {
        var req = new ProntuarioRequest(patientId, null, null, null, null);

        when(userRepo.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req, professionalId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void shouldSkipPatientValidationWhenAdmin() {
        var req = new ProntuarioRequest(patientId, null, null, null, null);

        when(prontuarioRepo.existsByPatientId(patientId)).thenReturn(false);
        when(prontuarioRepo.save(any())).thenReturn(prontuario);

        var resp = service.create(req, professionalId, true);

        assertThat(resp.patientId()).isEqualTo(patientId);
        verify(userRepo, never()).findById(any());
    }

    // --- findById ---

    @Test
    void shouldFindProntuarioById() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        var resp = service.findById(prontuario.getId(), professionalId, true, false, false);

        assertThat(resp.id()).isEqualTo(prontuario.getId());
        assertThat(resp.allergies()).isEqualTo("Pólen");
    }

    @Test
    void shouldAllowPatientToViewOwnProntuario() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        var resp = service.findById(prontuario.getId(), patientId, false, true, false);

        assertThat(resp.id()).isEqualTo(prontuario.getId());
    }

    @Test
    void shouldDenyPatientViewingOtherProntuario() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        assertThatThrownBy(() -> service.findById(prontuario.getId(), otherUserId, false, true, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access their own");
    }

    @Test
    void shouldAllowReceptionistViewAnyProntuario() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        var resp = service.findById(prontuario.getId(), otherUserId, false, false, true);

        assertThat(resp.id()).isEqualTo(prontuario.getId());
    }

    @Test
    void shouldThrowWhenProntuarioNotFound() {
        var fakeId = UUID.randomUUID();
        when(prontuarioRepo.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(fakeId, professionalId, true, false, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Prontuário not found");
    }

    // --- findByPatient ---

    @Test
    void shouldFindProntuarioByPatient() {
        when(prontuarioRepo.findByPatientId(patientId)).thenReturn(Optional.of(prontuario));

        var resp = service.findByPatient(patientId, professionalId, true, false, false);

        assertThat(resp.patientId()).isEqualTo(patientId);
    }

    @Test
    void shouldThrowWhenNoProntuarioForPatient() {
        when(prontuarioRepo.findByPatientId(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByPatient(patientId, professionalId, true, false, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Prontuário not found for this patient");
    }

    // --- update ---

    @Test
    void shouldUpdateProntuario() {
        var req = new ProntuarioRequest(patientId, "Novas alergias", null, null, "Nota atualizada");

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(prontuarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(prontuario.getId(), req, professionalId, false);

        assertThat(resp.allergies()).isEqualTo("Novas alergias");
        assertThat(resp.notes()).isEqualTo("Nota atualizada");
        assertThat(resp.chronicConditions()).isEqualTo("Asma");
    }

    @Test
    void shouldDenyUpdateWhenNotOwnerAndNotAdmin() {
        var req = new ProntuarioRequest(patientId, null, null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        assertThatThrownBy(() -> service.update(prontuario.getId(), req, otherUserId, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the responsible professional");
    }

    @Test
    void shouldAllowAdminToUpdateAnyProntuario() {
        var req = new ProntuarioRequest(patientId, "Nova alergia admin", null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(prontuarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(prontuario.getId(), req, otherUserId, true);

        assertThat(resp.allergies()).isEqualTo("Nova alergia admin");
    }

    // --- archive ---

    @Test
    void shouldArchiveProntuarioAsAdmin() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(prontuarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.archive(prontuario.getId(), true);

        assertThat(resp.status()).isEqualTo("ARCHIVED");
    }

    @Test
    void shouldDenyArchiveWhenNotAdmin() {
        assertThatThrownBy(() -> service.archive(prontuario.getId(), false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only admins can archive");
        verifyNoInteractions(prontuarioRepo);
    }

    // --- addEvolucao ---

    @Test
    void shouldAddEvolucao() {
        var req = new EvolucaoClinicaRequest(evolucao.getAppointmentId(), "Subjetivo", "Objetivo", "Avaliação", "Plano", "TCC");

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(evolucaoRepo.existsByAppointmentId(evolucao.getAppointmentId())).thenReturn(false);
        when(evolucaoRepo.save(any())).thenReturn(evolucao);

        var resp = service.addEvolucao(prontuario.getId(), req, professionalId, false);

        assertThat(resp.appointmentId()).isEqualTo(evolucao.getAppointmentId());
        assertThat(resp.techniques()).isEqualTo("TCC");
    }

    @Test
    void shouldDenyEvolucaoWhenNotOwner() {
        var req = new EvolucaoClinicaRequest(evolucao.getAppointmentId(), null, null, null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        assertThatThrownBy(() -> service.addEvolucao(prontuario.getId(), req, otherUserId, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the responsible professional");
    }

    @Test
    void shouldThrowWhenEvolucaoAlreadyExistsForAppointment() {
        var req = new EvolucaoClinicaRequest(evolucao.getAppointmentId(), null, null, null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(evolucaoRepo.existsByAppointmentId(evolucao.getAppointmentId())).thenReturn(true);

        assertThatThrownBy(() -> service.addEvolucao(prontuario.getId(), req, professionalId, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    // --- listEvolucoes ---

    @Test
    void shouldListEvolucoes() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(evolucaoRepo.findByProntuarioIdOrderByCreatedAtDesc(prontuario.getId()))
                .thenReturn(List.of(evolucao));

        var resp = service.listEvolucoes(prontuario.getId(), professionalId, true, false, false);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).techniques()).isEqualTo("TCC");
    }

    @Test
    void shouldDenyListEvolucoesWhenPatientNotOwner() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        assertThatThrownBy(() -> service.listEvolucoes(prontuario.getId(), otherUserId, false, true, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- createAnamnese ---

    @Test
    void shouldCreateAnamnese() {
        var req = new AnamneseRequest(prontuario.getId(), "Queixa principal", "HDA", "História pessoal",
                "História familiar", "História social", "Medicamentos");

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(anamneseRepo.findByProntuarioId(prontuario.getId())).thenReturn(Optional.empty());
        when(anamneseRepo.save(any())).thenReturn(anamnese);

        var resp = service.createAnamnese(req, professionalId, false);

        assertThat(resp.prontuarioId()).isEqualTo(prontuario.getId());
        assertThat(resp.chiefComplaint()).isEqualTo("Ansiedade");
    }

    @Test
    void shouldThrowWhenAnamneseAlreadyExists() {
        var req = new AnamneseRequest(prontuario.getId(), null, null, null, null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(anamneseRepo.findByProntuarioId(prontuario.getId())).thenReturn(Optional.of(anamnese));

        assertThatThrownBy(() -> service.createAnamnese(req, professionalId, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // --- findAnamnese ---

    @Test
    void shouldFindAnamnese() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(anamneseRepo.findByProntuarioId(prontuario.getId())).thenReturn(Optional.of(anamnese));

        var resp = service.findAnamnese(prontuario.getId(), professionalId, true, false, false);

        assertThat(resp.prontuarioId()).isEqualTo(prontuario.getId());
        assertThat(resp.chiefComplaint()).isEqualTo("Ansiedade");
    }

    @Test
    void shouldThrowWhenAnamneseNotFound() {
        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(anamneseRepo.findByProntuarioId(prontuario.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAnamnese(prontuario.getId(), professionalId, true, false, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Anamnesis not found");
    }

    // --- updateAnamnese ---

    @Test
    void shouldUpdateAnamnese() {
        var req = new AnamneseRequest(prontuario.getId(), "Nova queixa", null, null, null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));
        when(anamneseRepo.findByProntuarioId(prontuario.getId())).thenReturn(Optional.of(anamnese));
        when(anamneseRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.updateAnamnese(prontuario.getId(), req, professionalId, false);

        assertThat(resp.chiefComplaint()).isEqualTo("Nova queixa");
        assertThat(resp.historyIllness()).isEqualTo("Início há 6 meses");
    }

    @Test
    void shouldDenyUpdateAnamneseWhenNotOwner() {
        var req = new AnamneseRequest(prontuario.getId(), null, null, null, null, null, null);

        when(prontuarioRepo.findById(prontuario.getId())).thenReturn(Optional.of(prontuario));

        assertThatThrownBy(() -> service.updateAnamnese(prontuario.getId(), req, otherUserId, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the responsible professional");
    }
}
