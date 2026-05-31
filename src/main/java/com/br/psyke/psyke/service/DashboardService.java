package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.DashboardResponse;
import com.br.psyke.psyke.model.Appointment;
import com.br.psyke.psyke.model.Invoice;
import com.br.psyke.psyke.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patients;
    private final ProfessionalRepository professionals;
    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;

    public DashboardResponse getSummary(UUID clinicId) {
        var totalPatients = patients.countByClinicId(clinicId);
        var totalProfessionals = professionals.countByClinicIdAndActiveTrue(clinicId);

        var todayStart = LocalDate.now().atStartOfDay();
        var todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        var todayAppointments = appointments.countByClinicIdAndStartTimeBetweenGroupByStatus(
                clinicId, todayStart, todayEnd);

        var statusMap = new LinkedHashMap<String, Long>();
        long todayTotal = 0;
        for (var row : todayAppointments) {
            var status = ((Appointment.AppointmentStatus) row[0]).name();
            var count = (Long) row[1];
            statusMap.put(status, count);
            todayTotal += count;
        }

        var monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        var monthEnd = LocalDate.now().withDayOfMonth(1).plusMonths(1).atStartOfDay().minusNanos(1);
        var monthAppointments = appointments.countByClinicIdAndStartTimeBetweenGroupByStatus(
                clinicId, monthStart, monthEnd);

        long monthTotal = 0;
        for (var row : monthAppointments) {
            monthTotal += (Long) row[1];
        }

        var monthlyRevenue = invoices.sumByClinicIdAndStatus(clinicId, Invoice.InvoiceStatus.PAID);
        var pendingAmount = invoices.sumByClinicIdAndStatus(clinicId, Invoice.InvoiceStatus.PENDING);

        return new DashboardResponse(totalPatients, totalProfessionals, todayTotal,
                statusMap, monthlyRevenue, pendingAmount);
    }
}
