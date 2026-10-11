package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Dashboard summary response")
public record DashboardResponse(
    @Schema(description = "Total patients in the clinic") long totalPatients,
    @Schema(description = "Total active professionals") long totalProfessionals,
    @Schema(description = "Appointments scheduled for today") long todayAppointments,
    @Schema(description = "Today's appointments by status") Map<String, Long> appointmentsByStatus,
    @Schema(description = "Current month revenue from paid invoices") BigDecimal monthlyRevenue,
    @Schema(description = "Total pending invoices amount") BigDecimal pendingAmount,
    @Schema(description = "Active patients") long activePatients,
    @Schema(description = "Appointments in the current month") long monthAppointments
) {
    public DashboardResponse(long totalPatients, long totalProfessionals, long todayAppointments,
                             Map<String, Long> appointmentsByStatus, BigDecimal monthlyRevenue, BigDecimal pendingAmount) {
        this(totalPatients, totalProfessionals, todayAppointments, appointmentsByStatus, monthlyRevenue, pendingAmount,
             totalPatients, 0);
    }
}
