package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.ReportSummaryResponse;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.model.Patient;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reports are computed with aggregate SQL scoped by clinic and period. CSV cells are escaped against
 * formula injection (cells starting with = + - @ are prefixed with ').
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MAX_DAYS = 366;

    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;
    private final PatientRepository patients;
    private final ProfessionalRepository professionals;

    public ReportSummaryResponse summary(UUID clinicId, LocalDate from, LocalDate to) {
        validate(from, to);
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        var startTz = offset(from);
        var endTz = offset(to.plusDays(1));

        var byStatus = new LinkedHashMap<String, Long>();
        for (var s : Appointment.AppointmentStatus.values()) byStatus.put(s.name(), 0L);
        long total = 0;
        for (var row : appointments.countByClinicIdAndStartTimeBetweenGroupByStatus(clinicId, start, end)) {
            var count = (Long) row[1];
            byStatus.put(((Appointment.AppointmentStatus) row[0]).name(), count);
            total += count;
        }
        long done = byStatus.get("DONE");
        long noShow = byStatus.get("NO_SHOW");
        double attendance = done + noShow == 0 ? 0 : round(100.0 * done / (done + noShow));
        double cancellation = total == 0 ? 0 : round(100.0 * byStatus.get("CANCELLED") / total);

        var invoicesByStatus = new LinkedHashMap<String, Long>();
        for (var s : Invoice.InvoiceStatus.values()) invoicesByStatus.put(s.name(), 0L);
        var pending = BigDecimal.ZERO;
        for (var row : invoices.summarizeIssuedBetween(clinicId, startTz, endTz)) {
            var status = (Invoice.InvoiceStatus) row[0];
            invoicesByStatus.put(status.name(), (Long) row[1]);
            if (status == Invoice.InvoiceStatus.PENDING) pending = (BigDecimal) row[2];
        }

        var revenueByMonth = new ArrayList<ReportSummaryResponse.MonthlyAmount>();
        for (var row : invoices.paidRevenueByMonth(clinicId, startTz, endTz)) {
            revenueByMonth.add(new ReportSummaryResponse.MonthlyAmount((String) row[0], toDecimal(row[1])));
        }

        var doneRows = appointments.countDoneByProfessional(clinicId, start, end);
        var professionalIds = doneRows.stream().map(r -> (UUID) r[0]).collect(Collectors.toSet());
        Map<UUID, String> names = new HashMap<>();
        professionals.findAllById(professionalIds).forEach(p -> names.put(p.getId(), p.getFullName()));
        var sessions = doneRows.stream()
                .map(r -> new ReportSummaryResponse.ProfessionalSessions(
                        r[0].toString(), names.getOrDefault((UUID) r[0], "Profissional"), (Long) r[1]))
                .toList();

        return new ReportSummaryResponse(from.toString(), to.toString(),
                invoices.sumPaidBetween(clinicId, startTz, endTz), pending, invoicesByStatus,
                total, byStatus, attendance, cancellation,
                patients.countByClinicIdAndCreatedAtBetween(clinicId, startTz, endTz),
                patients.countByClinicIdAndStatus(clinicId, Patient.PatientStatus.ACTIVE),
                revenueByMonth, sessions);
    }

    public String exportAppointmentsCsv(UUID clinicId, LocalDate from, LocalDate to) {
        validate(from, to);
        var list = appointments.findForExport(clinicId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        var patientNames = patientNames(list.stream().map(Appointment::getPatientId).toList());
        var professionalNames = professionalNames(list.stream().map(Appointment::getProfessionalId).toList());

        var sb = new StringBuilder("﻿"); // BOM so Excel opens UTF-8 correctly
        sb.append("id,paciente,profissional,inicio,fim,status,observacoes\n");
        for (var a : list) {
            sb.append(a.getId()).append(',')
              .append(cell(patientNames.get(a.getPatientId()))).append(',')
              .append(cell(professionalNames.get(a.getProfessionalId()))).append(',')
              .append(a.getStartTime()).append(',')
              .append(a.getEndTime()).append(',')
              .append(a.getStatus()).append(',')
              .append(cell(a.getNotes())).append('\n');
        }
        return sb.toString();
    }

    public String exportInvoicesCsv(UUID clinicId, LocalDate from, LocalDate to) {
        validate(from, to);
        var list = invoices.findByClinicIdAndCreatedAtBetweenOrderByCreatedAt(clinicId, offset(from), offset(to.plusDays(1)));
        var patientNames = patientNames(list.stream().map(Invoice::getPatientId).toList());
        var professionalNames = professionalNames(list.stream().map(Invoice::getProfessionalId).toList());

        var sb = new StringBuilder("﻿");
        sb.append("id,paciente,profissional,valor,forma_pagamento,status,emitida_em,paga_em\n");
        for (var i : list) {
            sb.append(i.getId()).append(',')
              .append(cell(patientNames.get(i.getPatientId()))).append(',')
              .append(cell(professionalNames.get(i.getProfessionalId()))).append(',')
              .append(i.getAmount()).append(',')
              .append(cell(i.getPaymentMethod())).append(',')
              .append(i.getStatus()).append(',')
              .append(i.getCreatedAt()).append(',')
              .append(i.getPaymentDate() != null ? i.getPaymentDate() : "").append('\n');
        }
        return sb.toString();
    }

    private Map<UUID, String> patientNames(List<UUID> ids) {
        Map<UUID, String> map = new HashMap<>();
        patients.findAllById(ids.stream().filter(Objects::nonNull).collect(Collectors.toSet()))
                .forEach(p -> map.put(p.getId(), p.getFullName()));
        return map;
    }

    private Map<UUID, String> professionalNames(List<UUID> ids) {
        Map<UUID, String> map = new HashMap<>();
        professionals.findAllById(ids.stream().filter(Objects::nonNull).collect(Collectors.toSet()))
                .forEach(p -> map.put(p.getId(), p.getFullName()));
        return map;
    }

    private static void validate(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("A data final deve ser posterior à inicial");
        if (from.plusDays(MAX_DAYS).isBefore(to)) throw new IllegalArgumentException("Período máximo de 366 dias");
    }

    private static OffsetDateTime offset(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Quotes a CSV cell and neutralises spreadsheet formulas. */
    static String cell(String s) {
        if (s == null || s.isEmpty()) return "";
        var value = s;
        if ("=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
