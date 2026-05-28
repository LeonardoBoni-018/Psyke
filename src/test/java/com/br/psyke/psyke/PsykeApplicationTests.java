package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class PsykeApplicationTests {

    @Autowired private TestRestTemplate rest;
    @MockitoBean private KafkaTemplate<?, ?> kafkaTemplate;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/psyke_master");
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "123456");
        r.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret-12345");
    }

    @Test
    void shouldCreateTenant() {
        var slug = "c" + UUID.randomUUID().toString().substring(0, 6);
        var res = rest.postForEntity("/tenants",
            new CreateTenantRequest("C", slug, "BASIC", "a@c.com", "pass", "A", 1, 10),
            TenantResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().slug()).isEqualTo(slug);
    }

    @Test
    void shouldRejectDuplicateSlug() {
        var slug = "dup" + UUID.randomUUID().toString().substring(0, 6);
        rest.postForEntity("/tenants",
            new CreateTenantRequest("A", slug, "BASIC", "a@c.com", "pass", "A", 1, 10), TenantResponse.class);
        var res = rest.postForEntity("/tenants",
            new CreateTenantRequest("B", slug, "BASIC", "b@c.com", "pass", "B", 1, 10), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldLogin() {
        var slug = "lt" + UUID.randomUUID().toString().substring(0, 6);
        var t = rest.postForEntity("/tenants",
            new CreateTenantRequest("L", slug, "BASIC", "u@t.com", "S3cret", "U", 1, 10), TenantResponse.class);
        assertThat(t.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var l = rest.postForEntity("/auth/login",
            new LoginRequest("u@t.com", "S3cret", t.getBody().id()), TokenResponse.class);
        assertThat(l.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(l.getBody().accessToken()).isNotBlank();
        assertThat(l.getBody().refreshToken()).isNotBlank();
    }
}
