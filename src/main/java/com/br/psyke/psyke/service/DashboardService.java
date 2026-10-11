package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.DashboardResponse;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.model.Patient;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Every figure is a single aggregate query (COUNT/SUM) — nothing is loaded into memory. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patients;
    private final ProfessionalRepository professionals;
    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;

    public DashboardResponse getSummary(UUID clinicId) {
        var totalPatients = patients.countByClinicId(clinicId);
        var activePatients = patients.countByClinicIdAndStatus(clinicId, Patient.PatientStatus.ACTIVE);
        var totalProfessionals = professionals.countByClinicIdAndActiveTrue(clinicId);

        var today = LocalDate.now();
        var statusMap = new LinkedHashMap<String, Long>();
        long todayTotal = 0;
        for (var row : appointments.countByClinicIdAndStartTimeBetweenGroupByStatus(
                clinicId, today.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            var count = (Long) row[1];
            statusMap.put(((Appointment.AppointmentStatus) row[0]).name(), count);
            todayTotal += count;
        }

        var monthStart = today.withDayOfMonth(1);
        long monthTotal = 0;
        for (var row : appointments.countByClinicIdAndStartTimeBetweenGroupByStatus(
                clinicId, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay())) {
            monthTotal += (Long) row[1];
        }

        var zone = ZoneId.systemDefault();
        var monthlyRevenue = invoices.sumPaidBetween(clinicId,
                OffsetDateTime.of(monthStart.atStartOfDay(), zone.getRules().getOffset(monthStart.atStartOfDay())),
                OffsetDateTime.of(monthStart.plusMonths(1).atStartOfDay(),
                        zone.getRules().getOffset(monthStart.plusMonths(1).atStartOfDay())));
        var pendingAmount = invoices.sumByClinicIdAndStatus(clinicId, Invoice.InvoiceStatus.PENDING);

        return new DashboardResponse(totalPatients, totalProfessionals, todayTotal,
                statusMap, monthlyRevenue, pendingAmount, activePatients, monthTotal);
    }
}
