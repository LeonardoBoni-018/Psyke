package com.br.psyke.psyke.config;

import com.br.psyke.psyke.security.TenantAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")      private String url;
    @Value("${spring.datasource.username}") private String user;
    @Value("${spring.datasource.password}") private String pass;

    @Bean @Primary
    public DataSource master() {
        var ds = new HikariDataSource();
        ds.setJdbcUrl(url); ds.setUsername(user); ds.setPassword(pass);
        ds.setPoolName("master"); ds.setMaximumPoolSize(10); ds.setMinimumIdle(2);
        ds.setConnectionTimeout(30_000);
        return ds;
    }

    @Bean
    public TenantAwareDataSource tenantAware(DataSource master) { return new TenantAwareDataSource(master); }
}
