package com.br.psyke.psyke.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.AbstractDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Routes every connection to the schema of the tenant bound to {@link TenantContext}.
 *
 * <p>All tenants share ONE connection pool (the master pool). On checkout the connection's
 * {@code search_path} is set to {@code <tenant_schema>, public}, or to {@code public} when no tenant
 * is bound. Compared with one pool per tenant this keeps the number of database connections constant
 * as tenants grow (important on managed Postgres with low connection limits) and lets any API instance
 * serve a tenant created on another instance: unknown tenants are resolved lazily from
 * {@code public.tenants}.
 *
 * <p>{@code search_path} is session state, so the database must be reached through a direct
 * (non transaction-pooled) endpoint.
 */
@Slf4j
public class TenantAwareDataSource extends AbstractDataSource {

    private static final Pattern SAFE_SCHEMA = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");
    private static final String MASTER_SEARCH_PATH = "SET search_path TO public";

    private final DataSource master;
    private final Map<String, String> schemaByTenant = new ConcurrentHashMap<>();

    public TenantAwareDataSource(DataSource master) {
        this.master = master;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return bindSchema(master.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return bindSchema(master.getConnection(username, password));
    }

    /** Caches the schema of a tenant so the first request does not need a lookup. */
    public void registerTenant(String tenantId, String schema) {
        schemaByTenant.put(tenantId, requireSafe(schema));
        log.debug("Tenant {} mapped to schema {}", tenantId, schema);
    }

    public void evictTenant(String tenantId) {
        schemaByTenant.remove(tenantId);
    }

    private Connection bindSchema(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            var tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                statement.execute(MASTER_SEARCH_PATH);
            } else {
                statement.execute("SET search_path TO " + resolveSchema(connection, tenantId) + ", public");
            }
            return connection;
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    private String resolveSchema(Connection connection, String tenantId) throws SQLException {
        var cached = schemaByTenant.get(tenantId);
        if (cached != null) return cached;

        try (var ps = connection.prepareStatement(
                "SELECT schema_name FROM public.tenants WHERE id = CAST(? AS uuid) AND active = true")) {
            ps.setString(1, tenantId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Unknown or inactive tenant " + tenantId);
                var schema = requireSafe(rs.getString(1));
                schemaByTenant.put(tenantId, schema);
                return schema;
            }
        }
    }

    private static String requireSafe(String schema) {
        if (schema == null || !SAFE_SCHEMA.matcher(schema).matches())
            throw new IllegalArgumentException("Invalid schema name: " + schema);
        return schema;
    }
}
