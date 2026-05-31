---
name: test
description: Use when writing or fixing tests. Covers Mockito patterns, integration test setup, and @DynamicPropertySource configuration.
---

# Testing — Psyke Platform

## Test structure
- **Unit tests**: `@ExtendWith(MockitoExtension.class)` — mock all repositories and producers
- **Integration tests**: `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` + `@AutoConfigureTestRestTemplate`
- Kafka mocks: named `@MockitoBean(name = "appointmentKafkaTemplate")` and `@MockitoBean(name = "sessionKafkaTemplate")`

## Patterns

### Unit test
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock private SomeRepository repo;
    private MyService service;

    @BeforeEach
    void setUp() {
        service = new MyService(repo, ...);
    }
}
```

### Integration test with Kafka mocks
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class MyIntegrationTest {
    @MockitoBean(name = "appointmentKafkaTemplate")
    private KafkaTemplate<String, AppointmentEvent> appointmentKafkaTemplate;
    @MockitoBean(name = "sessionKafkaTemplate")
    private KafkaTemplate<String, SessionEvent> sessionKafkaTemplate;
}
```

### DynamicPropertySource
```java
@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.password", () -> "123456");
}
```

## Running
```powershell
.\mvnw.cmd test                                          # all tests
.\mvnw.cmd test -Dtest=UserServiceTest                    # single class
.\mvnw.cmd test -Dtest="UserServiceTest,AppointmentConflictTest"  # multiple classes
```
