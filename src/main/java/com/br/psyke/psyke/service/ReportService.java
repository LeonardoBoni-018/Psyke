package com.br.psyke.psyke.service;

import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;
    private final PatientRepository patients;

    public String exportAppointmentsCsv(UUID clinicId, LocalDate from, LocalDate to) {
        var fromDt = from.atStartOfDay();
        var toDt = to.atTime(LocalTime.MAX);
        var list = appointments.findAllByFilters(null, null, null, fromDt, toDt)
                .stream().filter(a -> clinicId.equals(a.getClinicId())).toList();

        var sb = new StringBuilder();
        sb.append("id,patient_id,professional_id,start_time,end_time,status,notes\n");
        for (var a : list) {
            sb.append(a.getId()).append(",")
              .append(a.getPatientId()).append(",")
              .append(a.getProfessionalId()).append(",")
              .append(a.getStartTime()).append(",")
              .append(a.getEndTime()).append(",")
              .append(a.getStatus()).append(",")
              .append(escape(a.getNotes())).append("\n");
        }
        return sb.toString();
    }

    public String exportInvoicesCsv(UUID clinicId, LocalDate from, LocalDate to) {
        var fromDt = from.atStartOfDay();
        var toDt = to.atTime(LocalTime.MAX);
        var page = invoices.findByClinicId(clinicId, org.springframework.data.domain.Pageable.unpaged());
        var list = page.getContent().stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().toLocalDate().isBefore(from)
                        && !i.getCreatedAt().toLocalDate().isAfter(to))
                .toList();

        var sb = new StringBuilder();
        sb.append("id,patient_id,professional_id,amount,payment_method,status,created_at\n");
        for (var i : list) {
            sb.append(i.getId()).append(",")
              .append(i.getPatientId()).append(",")
              .append(i.getProfessionalId()).append(",")
              .append(i.getAmount()).append(",")
              .append(nvl(i.getPaymentMethod())).append(",")
              .append(i.getStatus()).append(",")
              .append(i.getCreatedAt()).append("\n");
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
