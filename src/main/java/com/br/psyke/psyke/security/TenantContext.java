package com.br.psyke.psyke.security;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_CLINIC = new InheritableThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String id) { CURRENT_TENANT.set(id); }
    public static String getTenantId() { return CURRENT_TENANT.get(); }
    public static void setClinicId(String id) { CURRENT_CLINIC.set(id); }
    public static String getClinicId() { return CURRENT_CLINIC.get(); }
    public static void clear() { CURRENT_TENANT.remove(); CURRENT_CLINIC.remove(); }
    public static boolean hasTenant() { return CURRENT_TENANT.get() != null; }
}
