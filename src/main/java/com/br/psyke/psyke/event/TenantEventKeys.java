package com.br.psyke.psyke.event;

import com.br.psyke.psyke.security.TenantContext;

/**
 * Events are processed outside the request thread, so they must carry the tenant they belong to.
 * On Kafka the record key is {@code <tenantId>:<clinicId>:<entityId>}; in-process events use envelopes.
 */
final class TenantEventKeys {

    private TenantEventKeys() {}

    static String key(Object entityId) {
        return nullToEmpty(TenantContext.getTenantId()) + ":" + nullToEmpty(TenantContext.getClinicId()) + ":" + entityId;
    }

    static String tenantOf(String key) {
        return part(key, 0);
    }

    static String clinicOf(String key) {
        return part(key, 1);
    }

    private static String part(String key, int index) {
        if (key == null) return null;
        var parts = key.split(":", 3);
        if (parts.length < 3 || parts[index].isEmpty()) return null;
        return parts[index];
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
