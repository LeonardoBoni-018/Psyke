package com.br.psyke.psyke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Financial and clinical indicators for a period")
public record ReportSummaryResponse(
    String from,
    String to,
    @Schema(description = "Paid revenue in the period") BigDecimal revenue,
    @Schema(description = "Amount of invoices issued in the period still pending") BigDecimal pendingAmount,
    @Schema(description = "Invoices issued in the period by status: count") Map<String, Long> invoicesByStatus,
    long totalAppointments,
    @Schema(description = "Appointments in the period by status") Map<String, Long> appointmentsByStatus,
    @Schema(description = "DONE / (DONE + NO_SHOW), 0-100") double attendanceRate,
    @Schema(description = "CANCELLED / total, 0-100") double cancellationRate,
    long newPatients,
    long activePatients,
    List<MonthlyAmount> revenueByMonth,
    List<ProfessionalSessions> sessionsByProfessional
) {
    public record MonthlyAmount(String month, BigDecimal amount) {}

    public record ProfessionalSessions(String professionalId, String name, long sessions) {}
}
