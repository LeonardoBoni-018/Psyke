package com.br.psyke.psyke.service;

import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;
    private final PrescriptionRepository prescriptions;
    private final ConsentTermRepository consentTerms;
    private final PatientRepository patients;

    public byte[] exportAppointment(UUID appointmentId) {
        var appointment = appointments.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        var patient = patients.findById(appointment.getPatientId()).orElse(null);
        return generate(doc -> {
            addHeader(doc, "Detalhes do Agendamento");
            addField(doc, "ID", appointment.getId().toString());
            addField(doc, "Paciente", patient != null ? nvl(patient.getFullName()) : appointment.getPatientId().toString());
            addField(doc, "Início", appointment.getStartTime().toString());
            addField(doc, "Fim", appointment.getEndTime().toString());
            addField(doc, "Status", appointment.getStatus().name());
            addField(doc, "Observações", appointment.getNotes());
        });
    }

    public byte[] exportInvoice(UUID invoiceId) {
        var invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        var patient = patients.findById(invoice.getPatientId()).orElse(null);
        return generate(doc -> {
            addHeader(doc, "Fatura");
            addField(doc, "ID", invoice.getId().toString());
            addField(doc, "Paciente", patient != null ? nvl(patient.getFullName()) : invoice.getPatientId().toString());
            addField(doc, "Valor", "R$ " + invoice.getAmount());
            addField(doc, "Método Pagamento", nvl(invoice.getPaymentMethod()));
            addField(doc, "Status", invoice.getStatus().name());
            addField(doc, "Data Pagamento", invoice.getPaymentDate() != null ? invoice.getPaymentDate().toString() : "-");
            addField(doc, "Observações", invoice.getNotes());
        });
    }

    public byte[] exportPrescription(UUID prescriptionId) {
        var prescription = prescriptions.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));
        var patient = patients.findById(prescription.getPatientId()).orElse(null);
        return generate(doc -> {
            addHeader(doc, "Receita Médica");
            addField(doc, "ID", prescription.getId().toString());
            addField(doc, "Paciente", patient != null ? nvl(patient.getFullName()) : prescription.getPatientId().toString());
            addField(doc, "Medicação", prescription.getMedication());
            addField(doc, "Dosagem", nvl(prescription.getDosage()));
            addField(doc, "Instruções", nvl(prescription.getInstructions()));
            addField(doc, "Início", prescription.getStartDate() != null ? prescription.getStartDate().toString() : "-");
            addField(doc, "Fim", prescription.getEndDate() != null ? prescription.getEndDate().toString() : "-");
            addField(doc, "Ativo", prescription.isActive() ? "Sim" : "Não");
        });
    }

    public byte[] exportConsentTerm(UUID consentTermId) {
        var term = consentTerms.findById(consentTermId)
                .orElseThrow(() -> new RuntimeException("Consent term not found"));
        var patient = patients.findById(term.getPatientId()).orElse(null);
        return generate(doc -> {
            addHeader(doc, "Termo de Consentimento");
            addField(doc, "Título", term.getTitle());
            addField(doc, "Paciente", patient != null ? nvl(patient.getFullName()) : term.getPatientId().toString());
            addField(doc, "Conteúdo", term.getContent());
            addField(doc, "Assinado pelo Paciente", term.isSignedByPatient() ? "Sim" : "Não");
            addField(doc, "Assinado pelo Profissional", term.isSignedByProfessional() ? "Sim" : "Não");
            addField(doc, "Assinado em", term.getSignedAt() != null ? term.getSignedAt().toString() : "-");
            addField(doc, "Expira em", term.getExpiresAt() != null ? term.getExpiresAt().toString() : "-");
            addField(doc, "Ativo", term.isActive() ? "Sim" : "Não");
        });
    }

    @FunctionalInterface
    private interface PdfContent { void write(com.lowagie.text.Document doc) throws DocumentException; }

    private byte[] generate(PdfContent content) {
        var doc = new com.lowagie.text.Document();
        var out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            content.write(doc);
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private void addHeader(com.lowagie.text.Document doc, String title) throws DocumentException {
        doc.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        doc.add(new Paragraph(" "));
    }

    private void addField(com.lowagie.text.Document doc, String label, String value) throws DocumentException {
        doc.add(new Paragraph(label + ": " + (value != null ? value : "")));
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
