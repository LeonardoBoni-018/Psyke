package com.br.psyke.psyke;

import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private AppointmentRepository appointments;
    @Mock private InvoiceRepository invoices;
    @Mock private PatientRepository patients;
    private ReportService service;
    private UUID clinicId;

    @BeforeEach
    void setUp() {
        service = new ReportService(appointments, invoices, patients);
        clinicId = UUID.randomUUID();
    }

    @Test
    void shouldExportAppointmentsCsv() {
        var a = Appointment.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .startTime(LocalDateTime.of(2026, 6, 1, 9, 0))
                .endTime(LocalDateTime.of(2026, 6, 1, 9, 50))
                .status(Appointment.AppointmentStatus.SCHEDULED).build();

        when(appointments.findAllByFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of(a));

        var csv = service.exportAppointmentsCsv(clinicId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertThat(csv).contains("id,patient_id,professional_id");
        assertThat(csv).contains(a.getId().toString());
    }

    @Test
    void shouldExportInvoicesCsv() {
        var inv = Invoice.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .amount(new BigDecimal("250.00")).status(Invoice.InvoiceStatus.PAID)
                .build();
        inv.setCreatedAt(java.time.OffsetDateTime.now());

        when(invoices.findByClinicId(eq(clinicId), any())).thenReturn(new PageImpl<>(List.of(inv)));

        var csv = service.exportInvoicesCsv(clinicId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(csv).contains("id,patient_id,professional_id");
        assertThat(csv).contains("250.00");
    }
}
