package com.br.psyke.psyke.security;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> dataSources = new ConcurrentHashMap<>();

    public TenantAwareDataSource(DataSource master) {
        super.setDefaultTargetDataSource(master);
        dataSources.put("master", master);
        super.setTargetDataSources(dataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String id = TenantContext.getTenantId();
        return id != null ? id : "master";
    }

    public synchronized void addTenantDataSource(String tenantId, String schema, String url, String user, String pass) {
        if (dataSources.containsKey(tenantId)) return;
        var ds = new HikariDataSource();
        ds.setJdbcUrl(url + "&currentSchema=" + schema);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setPoolName("pool-" + tenantId);
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        ds.setConnectionTimeout(30_000);
        dataSources.put(tenantId, ds);
        super.setTargetDataSources(dataSources);
        super.afterPropertiesSet();
        log.info("DataSource for tenant {} (schema {})", tenantId, schema);
    }
}
