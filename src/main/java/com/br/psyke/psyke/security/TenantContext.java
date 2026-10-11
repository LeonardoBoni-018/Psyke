package com.br.psyke.psyke.security;

/**
 * Per-thread tenant/clinic holder. Deliberately a plain ThreadLocal (not inheritable):
 * pooled threads must never inherit a tenant from the thread that created them.
 * Code running outside an HTTP request must use {@link TenantScope}.
 */
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_CLINIC = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String id) { CURRENT_TENANT.set(id); }
    public static String getTenantId() { return CURRENT_TENANT.get(); }
    public static void setClinicId(String id) { CURRENT_CLINIC.set(id); }
    public static String getClinicId() { return CURRENT_CLINIC.get(); }
    public static void clear() { CURRENT_TENANT.remove(); CURRENT_CLINIC.remove(); }
    public static boolean hasTenant() { return CURRENT_TENANT.get() != null; }
}
