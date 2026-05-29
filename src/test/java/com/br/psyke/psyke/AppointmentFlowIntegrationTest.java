package com.br.psyke.psyke;

import com.br.psyke.psyke.dto.*;
import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.SessionEvent;
import com.br.psyke.psyke.model.*;
import com.br.psyke.psyke.repository.*;
import com.br.psyke.psyke.security.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.error.include-exception=true", "server.error.include-message=always"})
@AutoConfigureTestRestTemplate
@SuppressWarnings("unchecked")
class AppointmentFlowIntegrationTest {

    @Autowired private TestRestTemplate rest;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private SlotRepository slotRepo;
    @MockitoBean(name = "appointmentKafkaTemplate") private KafkaTemplate<String, AppointmentEvent> appointmentKafkaTemplate;
    @MockitoBean(name = "sessionKafkaTemplate") private KafkaTemplate<String, SessionEvent> sessionKafkaTemplate;

    private String adminToken;
    private UUID adminId;
    private UUID patientId;
    private UUID tenantId;
    private String tenantIdStr;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/psyke_master");
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "123456");
        r.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret-12345");
    }

    @BeforeEach
    void setUp() {
        var slug = "it" + UUID.randomUUID().toString().substring(0, 6);
        var tenantRes = rest.postForEntity("/tenants",
            new CreateTenantRequest("IT", slug, "BASIC", "admin@it.com", "S3cret", "Admin", 1, 10),
            TenantResponse.class);
        assertThat(tenantRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        tenantIdStr = tenantRes.getBody().id();
        tenantId = UUID.fromString(tenantIdStr);

        var loginRes = rest.postForEntity("/auth/login",
            new LoginRequest("admin@it.com", "S3cret", tenantIdStr),
            TokenResponse.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        adminToken = loginRes.getBody().accessToken();
        adminId = UUID.fromString(loginRes.getBody().userId());

        var         role = roleRepo.findByName("ROLE_USER").orElseGet(() ->
            roleRepo.save(Role.builder().name("ROLE_USER").description("User").build()));
        patientId = userRepo.save(User.builder()
            .tenantId(tenantId).fullName("Patient").email("patient@it.com")
            .passwordHash("dummy").active(true).emailVerified(true)
            .roles(Set.of(role)).build()).getId();

        when(appointmentKafkaTemplate.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sessionKafkaTemplate.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDebugCreateAppointment() {
        var nextMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        if (nextMonday.isBefore(LocalDate.now()) || nextMonday.equals(LocalDate.now()))
            nextMonday = nextMonday.plusWeeks(1);
        var slotStart = LocalDateTime.of(nextMonday, LocalTime.of(9, 0));
        var slotEnd = slotStart.plusMinutes(50);

        TenantContext.setTenantId(tenantIdStr);
        try {
            slotRepo.save(Slot.builder()
                .professionalId(adminId).startTime(slotStart).endTime(slotEnd)
                .status(Slot.SlotStatus.FREE).build());
        } finally {
            TenantContext.clear();
        }

        var res = rest.exchange(
            RequestEntity.post("/appointments")
                .header("Authorization", "Bearer " + adminToken)
                .body(new AppointmentRequest(patientId, adminId, null, slotStart, slotEnd, "Debug")),
            String.class);
        System.out.println("STATUS: " + res.getStatusCode());
        System.out.println("BODY: " + res.getBody());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldCompleteFullAppointmentLifecycle() {
        var nextMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        if (nextMonday.isBefore(LocalDate.now()) || nextMonday.equals(LocalDate.now()))
            nextMonday = nextMonday.plusWeeks(1);

        var slotStart = LocalDateTime.of(nextMonday, LocalTime.of(9, 0));
        var slotEnd = slotStart.plusMinutes(50);
        TenantContext.setTenantId(tenantIdStr);
        try {
            slotRepo.save(Slot.builder()
                .professionalId(adminId).startTime(slotStart).endTime(slotEnd)
                .status(Slot.SlotStatus.FREE).build());
        } finally {
            TenantContext.clear();
        }

        var aptRes = rest.exchange(
            RequestEntity.post("/appointments")
                .header("Authorization", "Bearer " + adminToken)
                .body(new AppointmentRequest(patientId, adminId, null, slotStart, slotEnd, "Integration test")),
            AppointmentResponse.class);
        assertThat(aptRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(aptRes.getBody().status()).isEqualTo("SCHEDULED");
        var aptId = aptRes.getBody().id();

        var confirmRes = rest.exchange(
            RequestEntity.patch("/appointments/{id}/status", aptId)
                .header("Authorization", "Bearer " + adminToken)
                .body(new StatusUpdateRequest(Appointment.AppointmentStatus.CONFIRMED, null)),
            AppointmentResponse.class);
        assertThat(confirmRes.getBody().status()).isEqualTo("CONFIRMED");

        var sessionRes = rest.exchange(
            RequestEntity.post("/appointments/{id}/session", aptId)
                .header("Authorization", "Bearer " + adminToken)
                .body(new CreateSessionRequest("Paciente apresentou melhora significativa")),
            AppointmentResponse.class);
        assertThat(sessionRes.getBody().status()).isEqualTo("DONE");
        assertThat(sessionRes.getBody().prontuario()).contains("melhora");

        var listRes = rest.exchange(
            RequestEntity.get("/appointments?professionalId={p}&from={f}&to={t}",
                adminId, nextMonday.atStartOfDay(), nextMonday.plusDays(1).atStartOfDay())
                .header("Authorization", "Bearer " + adminToken).build(),
            new ParameterizedTypeReference<List<AppointmentResponse>>() {});
        assertThat(listRes.getBody()).hasSize(1);

        var calRes = rest.exchange(
            RequestEntity.get("/appointments/calendar?from={f}&to={t}",
                nextMonday.atStartOfDay(), nextMonday.plusDays(1).atStartOfDay())
                .header("Authorization", "Bearer " + adminToken).build(),
            new ParameterizedTypeReference<List<CalendarEvent>>() {});
        assertThat(calRes.getBody()).hasSize(1);
        assertThat(calRes.getBody().get(0).extendedProps().get("status")).isEqualTo("DONE");

        verify(appointmentKafkaTemplate, atLeast(2)).send(any(), any(), any());
        verify(sessionKafkaTemplate, atLeast(1)).send(any(), any(), any());
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        var nextMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        if (nextMonday.isBefore(LocalDate.now()) || nextMonday.equals(LocalDate.now()))
            nextMonday = nextMonday.plusWeeks(1);
        var slotStart = LocalDateTime.of(nextMonday, LocalTime.of(9, 0));
        var slotEnd = slotStart.plusMinutes(50);

        TenantContext.setTenantId(tenantIdStr);
        try {
            slotRepo.save(Slot.builder()
                .professionalId(adminId).startTime(slotStart).endTime(slotEnd)
                .status(Slot.SlotStatus.FREE).build());
        } finally {
            TenantContext.clear();
        }

        var aptRes = rest.exchange(
            RequestEntity.post("/appointments")
                .header("Authorization", "Bearer " + adminToken)
                .body(new AppointmentRequest(patientId, adminId, null, slotStart, slotEnd, "State test")),
            AppointmentResponse.class);
        assertThat(aptRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var aptId = aptRes.getBody().id();

        var badRes = rest.exchange(
            RequestEntity.patch("/appointments/{id}/status", aptId)
                .header("Authorization", "Bearer " + adminToken)
                .body(new StatusUpdateRequest(Appointment.AppointmentStatus.DONE, null)),
            ErrorResponse.class);
        assertThat(badRes.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void shouldHandleCancellationAndWaitingList() {
        var nextMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        if (nextMonday.isBefore(LocalDate.now()) || nextMonday.equals(LocalDate.now()))
            nextMonday = nextMonday.plusWeeks(1);

        var slotStart = LocalDateTime.of(nextMonday, LocalTime.of(9, 0));
        var slotEnd = slotStart.plusMinutes(50);
        TenantContext.setTenantId(tenantIdStr);
        try {
            slotRepo.save(Slot.builder()
                .professionalId(adminId).startTime(slotStart).endTime(slotEnd)
                .status(Slot.SlotStatus.FREE).build());
        } finally {
            TenantContext.clear();
        }

        var aptRes = rest.exchange(
            RequestEntity.post("/appointments")
                .header("Authorization", "Bearer " + adminToken)
                .body(new AppointmentRequest(patientId, adminId, null, slotStart, slotEnd, "Cancel test")),
            AppointmentResponse.class);
        var aptId = aptRes.getBody().id();

        var cancelRes = rest.exchange(
            RequestEntity.patch("/appointments/{id}/status", aptId)
                .header("Authorization", "Bearer " + adminToken)
                .body(new StatusUpdateRequest(Appointment.AppointmentStatus.CANCELLED, "Patient no longer needs")),
            AppointmentResponse.class);
        assertThat(cancelRes.getBody().status()).isEqualTo("CANCELLED");

        var wlRes = rest.exchange(
            RequestEntity.post("/waiting-list")
                .header("Authorization", "Bearer " + adminToken)
                .body(new WaitingListRequest(patientId, adminId, 1)),
            WaitingListResponse.class);
        assertThat(wlRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(wlRes.getBody().status()).isEqualTo("WAITING");

        var listWl = rest.exchange(
            RequestEntity.get("/waiting-list?professionalId={p}", adminId)
                .header("Authorization", "Bearer " + adminToken).build(),
            new ParameterizedTypeReference<List<WaitingListResponse>>() {});
        assertThat(listWl.getBody()).hasSize(1);

        verify(appointmentKafkaTemplate, atLeast(2)).send(any(), any(), any());
    }
}
