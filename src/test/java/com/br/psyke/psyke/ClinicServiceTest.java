package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateClinicRequest;
import com.br.psyke.psyke.dto.UpdateClinicRequest;
import com.br.psyke.psyke.exception.DuplicateResourceException;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Clinic;
import com.br.psyke.psyke.repository.ClinicRepository;
import com.br.psyke.psyke.service.ClinicService;
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
class ClinicServiceTest {

    @Mock private ClinicRepository clinics;
    private ClinicService service;
    private Clinic existingClinic;

    @BeforeEach
    void setUp() {
        service = new ClinicService(clinics);
        existingClinic = Clinic.builder()
                .id(UUID.randomUUID()).name("Psyke Centro")
                .cnpj("11222333000181").crp("06/12345")
                .phone("+5511988887777").active(true)
                .build();
    }

    @Test
    void shouldCreateClinic() {
        var req = new CreateClinicRequest("Nova Clínica", null, "99888777000155",
                "06/54321", null, "+5511977776666", null, null);
        var saved = Clinic.builder().id(UUID.randomUUID())
                .name("Nova Clínica").cnpj("99888777000155")
                .crp("06/54321").phone("+5511977776666").active(true)
                .build();

        when(clinics.existsByCnpj("99888777000155")).thenReturn(false);
        when(clinics.save(any())).thenReturn(saved);

        var resp = service.create(req);
        assertThat(resp.name()).isEqualTo("Nova Clínica");
        assertThat(resp.cnpj()).isEqualTo("99888777000155");
        assertThat(resp.active()).isTrue();
    }

    @Test
    void shouldThrowWhenCnpjAlreadyExists() {
        var req = new CreateClinicRequest("Outra", null, "11222333000181",
                null, null, null, null, null);

        when(clinics.existsByCnpj("11222333000181")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void shouldFindClinicById() {
        when(clinics.findByIdAndActiveTrue(existingClinic.getId()))
                .thenReturn(Optional.of(existingClinic));

        var resp = service.findById(existingClinic.getId());
        assertThat(resp.name()).isEqualTo("Psyke Centro");
    }

    @Test
    void shouldThrowWhenClinicNotFound() {
        var id = UUID.randomUUID();
        when(clinics.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllClinicsWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingClinic));

        when(clinics.findAll(pageable)).thenReturn(page);

        var resp = service.findAll(pageable);
        assertThat(resp).hasSize(1);
        assertThat(resp.getContent().get(0).name()).isEqualTo("Psyke Centro");
    }

    @Test
    void shouldUpdateClinic() {
        var id = existingClinic.getId();
        var req = new UpdateClinicRequest("Psyke Updated", null, null,
                null, null, null, null, null, null);

        when(clinics.findById(id)).thenReturn(Optional.of(existingClinic));
        when(clinics.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req);
        assertThat(resp.name()).isEqualTo("Psyke Updated");
    }

    @Test
    void shouldThrowOnUpdateWhenCnpjConflict() {
        var id = existingClinic.getId();
        var req = new UpdateClinicRequest(null, null, "99888777000155",
                null, null, null, null, null, null);

        when(clinics.findById(id)).thenReturn(Optional.of(existingClinic));
        when(clinics.existsByCnpjAndIdNot("99888777000155", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void shouldDeactivateClinic() {
        var id = existingClinic.getId();
        when(clinics.findById(id)).thenReturn(Optional.of(existingClinic));

        service.delete(id);
        assertThat(existingClinic.isActive()).isFalse();
        verify(clinics).save(existingClinic);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(clinics.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateAllFields() {
        var id = existingClinic.getId();
        var req = new UpdateClinicRequest("New Name", "New Legal", "99888777000155",
                "06/99999", "Rua A, 123", "+5511977776666",
                "https://logo.com/img.png", "{\"theme\":\"dark\"}", true);

        when(clinics.findById(id)).thenReturn(Optional.of(existingClinic));
        when(clinics.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req);
        assertThat(resp.name()).isEqualTo("New Name");
        assertThat(resp.legalName()).isEqualTo("New Legal");
        assertThat(resp.cnpj()).isEqualTo("99888777000155");
        assertThat(resp.crp()).isEqualTo("06/99999");
        assertThat(resp.address()).isEqualTo("Rua A, 123");
        assertThat(resp.phone()).isEqualTo("+5511977776666");
        assertThat(resp.logoUrl()).isEqualTo("https://logo.com/img.png");
        assertThat(resp.settings()).isEqualTo("{\"theme\":\"dark\"}");
        assertThat(resp.active()).isTrue();
    }
}
