package com.br.psyke.psyke.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlywayConfig {

    private final DataSource master;

    @Bean
    public CommandLineRunner migrate() {
        return a -> {
            Flyway.configure().dataSource(master).schemas("public")
                .locations("classpath:db/migration/master").baselineOnMigrate(true).load().migrate();
            log.info("Master migrations done");
        };
    }
}
