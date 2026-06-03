package com.br.psyke.psyke.config;

import com.br.psyke.psyke.model.Tenant;
import com.br.psyke.psyke.repository.TenantRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    @Resource(name = "master")
    private DataSource master;

    @Bean
    public CommandLineRunner migrate(TenantRepository tenantRepo) {
        return a -> {
            var masterFlyway = Flyway.configure().dataSource(master).schemas("public")
                    .locations("classpath:db/migration/master").baselineOnMigrate(true).load();
            var masterResult = masterFlyway.migrate();
            log.info("Master Flyway migrations: {} applied, {} skipped",
                    masterResult.migrationsExecuted, masterResult.warnings.size());

            var tenants = tenantRepo.findAll().stream().filter(Tenant::isActive).toList();
            for (var t : tenants) {
                try {
                    var f = Flyway.configure().dataSource(master).schemas(t.getSchemaName())
                            .locations("classpath:db/migration/tenant").baselineOnMigrate(true).load();
                    var r = f.migrate();
                    if (r.migrationsExecuted > 0) {
                        log.info("Tenant '{}' ({}): {} migrations applied",
                                t.getName(), t.getSchemaName(), r.migrationsExecuted);
                    }
                } catch (Exception e) {
                    log.warn("Failed to migrate tenant '{}' ({}): {}",
                            t.getName(), t.getSchemaName(), e.getMessage());
                }
            }
        };
    }
}
