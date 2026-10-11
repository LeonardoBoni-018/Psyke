package com.br.psyke.psyke;

import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private PatientRepository patients;
    @Mock private ProfessionalRepository professionals;
    @Mock private AppointmentRepository appointments;
    @Mock private InvoiceRepository invoices;
    private DashboardService service;
    private UUID clinicId;

    @BeforeEach
    void setUp() {
        service = new DashboardService(patients, professionals, appointments, invoices);
        clinicId = UUID.randomUUID();
    }

    @Test
    void shouldReturnDashboardSummary() {
        when(patients.countByClinicId(clinicId)).thenReturn(10L);
        when(professionals.countByClinicIdAndActiveTrue(clinicId)).thenReturn(3L);
        when(appointments.countByClinicIdAndStartTimeBetweenGroupByStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(invoices.sumPaidBetween(eq(clinicId), any(), any()))
                .thenReturn(new BigDecimal("5000.00"));
        when(invoices.sumByClinicIdAndStatus(clinicId, Invoice.InvoiceStatus.PENDING))
                .thenReturn(new BigDecimal("1500.00"));

        var resp = service.getSummary(clinicId);
        assertThat(resp.totalPatients()).isEqualTo(10);
        assertThat(resp.totalProfessionals()).isEqualTo(3);
        assertThat(resp.monthlyRevenue()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(resp.pendingAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }
}
