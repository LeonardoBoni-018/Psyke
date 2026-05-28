package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PsykeApplicationTests {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("psyke_master").withUsername("test").withPassword("test");

    @Autowired private TestRestTemplate rest;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret-12345");
    }

    @Test
    void shouldCreateTenant() {
        var res = rest.postForEntity("/tenants",
            new CreateTenantRequest("C", "c1", "BASIC", "a@c.com", "pass", "A", 1, 10),
            TenantResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().slug()).isEqualTo("c1");
    }

    @Test
    void shouldRejectDuplicateSlug() {
        rest.postForEntity("/tenants",
            new CreateTenantRequest("A", "dupX", "BASIC", "a@c.com", "pass", "A", 1, 10), TenantResponse.class);
        var res = rest.postForEntity("/tenants",
            new CreateTenantRequest("B", "dupX", "BASIC", "b@c.com", "pass", "B", 1, 10), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldLogin() {
        var t = rest.postForEntity("/tenants",
            new CreateTenantRequest("L", "lt", "BASIC", "u@t.com", "S3cret", "U", 1, 10), TenantResponse.class);
        assertThat(t.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var l = rest.postForEntity("/auth/login",
            new LoginRequest("u@t.com", "S3cret", t.getBody().id()), TokenResponse.class);
        assertThat(l.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(l.getBody().accessToken()).isNotBlank();
        assertThat(l.getBody().refreshToken()).isNotBlank();
    }
}
