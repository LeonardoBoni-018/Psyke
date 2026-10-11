package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.CreateInvoiceRequest;
import com.br.psyke.psyke.dto.InvoiceResponse;
import com.br.psyke.psyke.dto.UpdateInvoiceRequest;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.InvoiceRepository;
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final PatientRepository patients;
    private final ProfessionalRepository professionals;

    public Page<InvoiceResponse> findAll(UUID clinicId, UUID patientId, String status, Pageable pageable) {
        var sorted = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        var st = status != null && !status.isBlank()
                ? Invoice.InvoiceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)) : null;

        Page<Invoice> page;
        if (patientId != null && st != null) page = invoices.findByClinicIdAndPatientIdAndStatus(clinicId, patientId, st, sorted);
        else if (patientId != null) page = invoices.findByClinicIdAndPatientId(clinicId, patientId, sorted);
        else if (st != null) page = invoices.findByClinicIdAndStatus(clinicId, st, sorted);
        else page = invoices.findByClinicId(clinicId, sorted);
        return withNames(page);
    }

    public InvoiceResponse findById(UUID id, UUID clinicId) {
        return invoices.findByIdAndClinicId(id, clinicId)
                .map(i -> withNames(new org.springframework.data.domain.PageImpl<>(java.util.List.of(i))).getContent().get(0))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest req, UUID clinicId) {
        patients.findByIdAndClinicId(req.patientId(), clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
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
        if (req.status() != null) {
            var status = Invoice.InvoiceStatus.valueOf(req.status().trim().toUpperCase(Locale.ROOT));
            if (status == Invoice.InvoiceStatus.PAID && invoice.getPaymentDate() == null && req.paymentDate() == null)
                invoice.setPaymentDate(OffsetDateTime.now());
            invoice.setStatus(status);
        }
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

    /** Resolves patient/professional names for a whole page with two queries. */
    private Page<InvoiceResponse> withNames(Page<Invoice> page) {
        if (page.isEmpty()) return page.map(this::toResponse);
        var patientIds = page.stream().map(Invoice::getPatientId).collect(Collectors.toSet());
        var professionalIds = page.stream().map(Invoice::getProfessionalId).collect(Collectors.toSet());
        Map<UUID, String> patientNames = new HashMap<>();
        patients.findAllById(patientIds).forEach(p -> patientNames.put(p.getId(), p.getFullName()));
        Map<UUID, String> professionalNames = new HashMap<>();
        professionals.findAllById(professionalIds).forEach(p -> professionalNames.put(p.getId(), p.getFullName()));
        return page.map(i -> toResponse(i, patientNames.get(i.getPatientId()), professionalNames.get(i.getProfessionalId())));
    }

    private InvoiceResponse toResponse(Invoice i) {
        return toResponse(i, null, null);
    }

    private InvoiceResponse toResponse(Invoice i, String patientName, String professionalName) {
        return new InvoiceResponse(
                i.getId().toString(), i.getClinicId(), i.getAppointmentId(),
                i.getPatientId(), i.getProfessionalId(), i.getAmount(),
                i.getPaymentMethod(), i.getPaymentDate(),
                i.getStatus().name(), i.getNotes(),
                i.getCreatedAt(), i.getUpdatedAt(), patientName, professionalName);
    }
}
