package com.br.psyke.psyke;

import com.br.psyke.psyke.model.AuditLog;
import com.br.psyke.psyke.repository.AuditLogRepository;
import com.br.psyke.psyke.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository logs;
    private AuditLogService service;
    private UUID clinicId;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(logs);
        clinicId = UUID.randomUUID();
    }

    @Test
    void shouldLogAuditEntry() {
        var saved = AuditLog.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .entityType("PATIENT").entityId(UUID.randomUUID())
                .action("CREATED").userId(UUID.randomUUID()).details("{\"name\":\"João\"}")
                .build();

        when(logs.save(any())).thenReturn(saved);

        var resp = service.log(clinicId, "PATIENT", UUID.randomUUID(), "CREATED", UUID.randomUUID(), "{\"name\":\"João\"}");
        assertThat(resp.getAction()).isEqualTo("CREATED");
        assertThat(resp.getEntityType()).isEqualTo("PATIENT");
    }

    @Test
    void shouldFindAllWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var entry = AuditLog.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .entityType("PATIENT").action("CREATED").build();
        var page = new PageImpl<>(List.of(entry));

        when(logs.findByClinicId(clinicId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, null, null, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldFilterByEntityType() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(AuditLog.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .entityType("APPOINTMENT").action("UPDATED").build()));

        when(logs.findByClinicIdAndEntityType(clinicId, "APPOINTMENT", pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, "APPOINTMENT", null, pageable);
        assertThat(resp).hasSize(1);
    }
}
