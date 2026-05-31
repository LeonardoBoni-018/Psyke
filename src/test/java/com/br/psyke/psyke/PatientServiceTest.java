package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreatePatientRequest;
import com.br.psyke.psyke.dto.UpdatePatientRequest;
import com.br.psyke.psyke.exception.DuplicateResourceException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Patient;
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.service.PatientService;
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
class PatientServiceTest {

    @Mock private PatientRepository patients;
    private PatientService service;
    private UUID clinicId;
    private Patient existingPatient;

    @BeforeEach
    void setUp() {
        service = new PatientService(patients);
        clinicId = UUID.randomUUID();
        existingPatient = Patient.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .fullName("João Silva").cpf("12345678900")
                .email("joao@test.com").phone("+5511988887777")
                .status(Patient.PatientStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldCreatePatient() {
        var req = new CreatePatientRequest("Maria Souza", LocalDate.of(1990, 5, 15),
                "98765432100", "Feminino", "Casado", "Médica",
                "+5511977776666", "maria@test.com", "Unimed", "123", "Dr. Pedro");
        var saved = Patient.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .fullName("Maria Souza").birthDate(LocalDate.of(1990, 5, 15))
                .cpf("98765432100").gender("Feminino").maritalStatus("Casado")
                .occupation("Médica").phone("+5511977776666").email("maria@test.com")
                .insurance("Unimed").insuranceNumber("123").referredBy("Dr. Pedro")
                .status(Patient.PatientStatus.ACTIVE).build();

        when(patients.existsByClinicIdAndCpf(clinicId, "98765432100")).thenReturn(false);
        when(patients.existsByClinicIdAndEmail(clinicId, "maria@test.com")).thenReturn(false);
        when(patients.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.fullName()).isEqualTo("Maria Souza");
        assertThat(resp.cpf()).isEqualTo("98765432100");
        assertThat(resp.status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldThrowWhenCpfAlreadyExists() {
        var req = new CreatePatientRequest("Outro", null, "12345678900",
                null, null, null, null, null, null, null, null);

        when(patients.existsByClinicIdAndCpf(clinicId, "12345678900")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, clinicId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        var req = new CreatePatientRequest("Outro", null, null,
                null, null, null, null, "joao@test.com", null, null, null);

        when(patients.existsByClinicIdAndEmail(clinicId, "joao@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, clinicId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void shouldFindPatientById() {
        when(patients.findByIdAndClinicId(existingPatient.getId(), clinicId))
                .thenReturn(Optional.of(existingPatient));

        var resp = service.findById(existingPatient.getId(), clinicId);
        assertThat(resp.fullName()).isEqualTo("João Silva");
    }

    @Test
    void shouldThrowWhenPatientNotFound() {
        var id = UUID.randomUUID();
        when(patients.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllPatientsWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingPatient));

        when(patients.findByClinicId(clinicId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, pageable);
        assertThat(resp).hasSize(1);
        assertThat(resp.getContent().get(0).fullName()).isEqualTo("João Silva");
    }

    @Test
    void shouldUpdatePatient() {
        var id = existingPatient.getId();
        var req = new UpdatePatientRequest("João Updated", null, null,
                null, null, null, null, null, null, null, null, null);

        when(patients.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingPatient));
        when(patients.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, clinicId);
        assertThat(resp.fullName()).isEqualTo("João Updated");
    }

    @Test
    void shouldThrowOnUpdateWhenCpfConflict() {
        var id = existingPatient.getId();
        var req = new UpdatePatientRequest(null, null, "99988877766",
                null, null, null, null, null, null, null, null, null);

        when(patients.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingPatient));
        when(patients.existsByClinicIdAndCpfAndIdNot(clinicId, "99988877766", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, req, clinicId))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void shouldDeletePatient() {
        var id = existingPatient.getId();
        when(patients.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingPatient));

        service.delete(id, clinicId);
        verify(patients).delete(existingPatient);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(patients.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
