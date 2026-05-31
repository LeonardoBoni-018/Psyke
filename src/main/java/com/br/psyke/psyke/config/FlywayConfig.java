package com.br.psyke.psyke.config;

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
    public CommandLineRunner migrate() {
        return a -> {
            var flyway = Flyway.configure().dataSource(master).schemas("public")
                    .locations("classpath:db/migration/master").baselineOnMigrate(true).load();
            var result = flyway.migrate();
            log.info("Master Flyway migrations: {} applied, {} skipped",
                    result.migrationsExecuted, result.warnings.size());
        };
    }
}
