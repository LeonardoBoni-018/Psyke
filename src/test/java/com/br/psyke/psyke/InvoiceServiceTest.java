package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.CreateInvoiceRequest;
import com.br.psyke.psyke.dto.UpdateInvoiceRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.InvoiceRepository;
import com.br.psyke.psyke.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoices;
    private InvoiceService service;
    private UUID clinicId;
    private Invoice existingInvoice;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(invoices);
        clinicId = UUID.randomUUID();
        existingInvoice = Invoice.builder()
                .id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(UUID.randomUUID()).professionalId(UUID.randomUUID())
                .amount(new BigDecimal("250.00")).status(Invoice.InvoiceStatus.PENDING)
                .build();
    }

    @Test
    void shouldCreateInvoice() {
        var req = new CreateInvoiceRequest(null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("300.00"), "credit_card", null, "Session payment");
        var saved = Invoice.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .patientId(req.patientId()).professionalId(req.professionalId())
                .amount(req.amount()).paymentMethod("credit_card")
                .status(Invoice.InvoiceStatus.PENDING).notes("Session payment").build();

        when(invoices.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(resp.status()).isEqualTo("PENDING");
    }

    @Test
    void shouldFindById() {
        when(invoices.findByIdAndClinicId(existingInvoice.getId(), clinicId))
                .thenReturn(Optional.of(existingInvoice));

        var resp = service.findById(existingInvoice.getId(), clinicId);
        assertThat(resp.status()).isEqualTo("PENDING");
    }

    @Test
    void shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(invoices.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllWithPagination() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingInvoice));

        when(invoices.findByClinicId(clinicId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, null, null, pageable);
        assertThat(resp).hasSize(1);
        assertThat(resp.getContent().get(0).amount()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void shouldFindByPatientId() {
        var patientId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(existingInvoice));

        when(invoices.findByClinicIdAndPatientId(clinicId, patientId, pageable)).thenReturn(page);

        var resp = service.findAll(clinicId, patientId, null, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldUpdateInvoice() {
        var id = existingInvoice.getId();
        var req = new UpdateInvoiceRequest(new BigDecimal("200.00"), "pix",
                OffsetDateTime.now(), "PAID", "Paid via Pix");

        when(invoices.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingInvoice));
        when(invoices.save(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.update(id, req, clinicId);
        assertThat(resp.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(resp.status()).isEqualTo("PAID");
        assertThat(resp.paymentMethod()).isEqualTo("pix");
    }

    @Test
    void shouldThrowOnUpdateWhenNotFound() {
        var id = UUID.randomUUID();
        when(invoices.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateInvoiceRequest(null, null, null, null, null), clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteInvoice() {
        var id = existingInvoice.getId();
        when(invoices.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.of(existingInvoice));

        service.delete(id, clinicId);
        verify(invoices).delete(existingInvoice);
    }

    @Test
    void shouldThrowOnDeleteWhenNotFound() {
        var id = UUID.randomUUID();
        when(invoices.findByIdAndClinicId(id, clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
