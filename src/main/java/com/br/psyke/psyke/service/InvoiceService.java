package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateInvoiceRequest;
import com.br.psyke.psyke.dto.InvoiceResponse;
import com.br.psyke.psyke.dto.UpdateInvoiceRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoices;

    public Page<InvoiceResponse> findAll(UUID clinicId, UUID patientId, String status, Pageable pageable) {
        if (patientId != null)
            return invoices.findByClinicIdAndPatientId(clinicId, patientId, pageable).map(this::toResponse);
        if (status != null)
            return invoices.findByClinicIdAndStatus(clinicId, Invoice.InvoiceStatus.valueOf(status), pageable).map(this::toResponse);
        return invoices.findByClinicId(clinicId, pageable).map(this::toResponse);
    }

    public InvoiceResponse findById(UUID id, UUID clinicId) {
        return invoices.findByIdAndClinicId(id, clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest req, UUID clinicId) {
        var invoice = Invoice.builder()
                .clinicId(clinicId)
                .appointmentId(req.appointmentId())
                .patientId(req.patientId())
                .professionalId(req.professionalId())
                .amount(req.amount())
                .paymentMethod(req.paymentMethod())
                .paymentDate(req.paymentDate())
                .notes(req.notes())
                .build();
        invoice = invoices.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse update(UUID id, UpdateInvoiceRequest req, UUID clinicId) {
        var invoice = invoices.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (req.amount() != null) invoice.setAmount(req.amount());
        if (req.paymentMethod() != null) invoice.setPaymentMethod(req.paymentMethod());
        if (req.paymentDate() != null) invoice.setPaymentDate(req.paymentDate());
        if (req.status() != null) invoice.setStatus(Invoice.InvoiceStatus.valueOf(req.status()));
        if (req.notes() != null) invoice.setNotes(req.notes());

        invoice = invoices.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public void delete(UUID id, UUID clinicId) {
        var invoice = invoices.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoices.delete(invoice);
    }

    private InvoiceResponse toResponse(Invoice i) {
        return new InvoiceResponse(
                i.getId().toString(), i.getClinicId(), i.getAppointmentId(),
                i.getPatientId(), i.getProfessionalId(), i.getAmount(),
                i.getPaymentMethod(), i.getPaymentDate(),
                i.getStatus().name(), i.getNotes(),
                i.getCreatedAt(), i.getUpdatedAt());
    }
}
