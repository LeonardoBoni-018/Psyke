package com.br.psyke.psyke.config;

import com.br.psyke.psyke.repository.TenantRepository;
import com.br.psyke.psyke.security.TenantAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import javax.sql.DataSource;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")      private String url;
    @Value("${spring.datasource.username}") private String user;
    @Value("${spring.datasource.password}") private String pass;
    @Value("${psyke.db.pool-size:10}")      private int poolSize;
    @Value("${psyke.db.min-idle:2}")        private int minIdle;

    @Bean
    public DataSource master() {
        var db = DatabaseUrl.parse(url, user, pass);
        var ds = new HikariDataSource();
        ds.setJdbcUrl(db.jdbcUrl());
        ds.setUsername(db.username());
        ds.setPassword(db.password());
        ds.setPoolName("psyke");
        ds.setMaximumPoolSize(poolSize);
        ds.setMinimumIdle(Math.min(minIdle, poolSize));
        ds.setConnectionTimeout(10_000);
        ds.setIdleTimeout(300_000);
        ds.setMaxLifetime(1_500_000);
        ds.setKeepaliveTime(240_000);
        ds.addDataSourceProperty("reWriteBatchedInserts", "true");
        ds.addDataSourceProperty("ApplicationName", "psyke-api");
        return ds;
    }

    @Bean @Primary
    public TenantAwareDataSource tenantAware(@Qualifier("master") DataSource master) {
        return new TenantAwareDataSource(master);
    }

    /** Warms the tenant → schema cache. Runs after the Flyway runner so the tables exist. */
    @Bean
    @Order(2)
    public CommandLineRunner registerTenantDataSources(TenantAwareDataSource ds, TenantRepository tenantRepo) {
        return a -> {
            int count = 0;
            for (var t : tenantRepo.findAll()) {
                if (t.isActive()) {
                    ds.registerTenant(t.getId().toString(), t.getSchemaName());
                    count++;
                }
            }
            log.info("Registered {} active tenants", count);
        };
    }

    /**
     * Accepts both JDBC URLs and the {@code postgres://user:pass@host:port/db?sslmode=require}
     * form that Neon, Render and Railway hand out.
     */
    record DatabaseUrl(String jdbcUrl, String username, String password) {
        static DatabaseUrl parse(String raw, String user, String pass) {
            if (raw.startsWith("jdbc:")) return new DatabaseUrl(raw, user, pass);
            if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://"))
                throw new IllegalStateException("Unsupported database URL scheme");
            var uri = java.net.URI.create(raw.replaceFirst("^postgres(ql)?://", "http://"));
            var username = user;
            var password = pass;
            if (uri.getRawUserInfo() != null) {
                var parts = uri.getRawUserInfo().split(":", 2);
                username = java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8);
                if (parts.length > 1)
                    password = java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
            }
            var port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            var query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
            return new DatabaseUrl("jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + query,
                    username, password);
        }
    }
}
