package com.br.psyke.psyke.controller;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.repository.PatientRepository;
import com.br.psyke.psyke.repository.UserRepository;
import com.br.psyke.psyke.security.TenantContext;
import com.br.psyke.psyke.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@Tag(name = "Patient Portal", description = "Patient-facing endpoints to view own data")
public class PatientPortalController {

    private final UserRepository users;
    private final PatientRepository patients;
    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;
    private final InvoiceService invoiceService;

    private UUID resolvePatientId(Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEmail() == null)
            throw new ResourceNotFoundException("User has no email");
        var clinicId = UUID.fromString(TenantContext.getClinicId());
        var patient = patients.findByClinicIdAndEmail(clinicId, user.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for this user"));
        return patient.getId();
    }

    @GetMapping("/appointments")
    @Operation(summary = "List my appointments")
    public ResponseEntity<?> myAppointments(Authentication auth, Pageable pageable) {
        var patientId = resolvePatientId(auth);
        var list = appointmentService.findAll(
                null, patientId, null, null, null);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/prescriptions")
    @Operation(summary = "List my prescriptions")
    public ResponseEntity<Page<PrescriptionResponse>> myPrescriptions(Authentication auth, Pageable pageable) {
        var patientId = resolvePatientId(auth);
        return ResponseEntity.ok(prescriptionService.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, pageable));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List my invoices")
    public ResponseEntity<Page<InvoiceResponse>> myInvoices(Authentication auth, Pageable pageable) {
        var patientId = resolvePatientId(auth);
        return ResponseEntity.ok(invoiceService.findAll(
                UUID.fromString(TenantContext.getClinicId()), patientId, null, pageable));
    }
}
