package com.br.psyke.psyke.security;

/**
 * Runs code with a tenant bound to the current thread and restores the previous binding afterwards.
 * Use it in Kafka listeners, async event handlers and scheduled jobs.
 */
public final class TenantScope {

    private TenantScope() {}

    public static void run(String tenantId, String clinicId, Runnable action) {
        var previousTenant = TenantContext.getTenantId();
        var previousClinic = TenantContext.getClinicId();
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setClinicId(clinicId);
            action.run();
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
                TenantContext.setClinicId(previousClinic);
            }
        }
    }
}
