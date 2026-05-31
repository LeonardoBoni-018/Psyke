---
name: crud
description: Use when creating a new CRUD resource (entity/repository/service/controller/DTOs). Covers multi-tenancy, pagination, exception handling, and testing patterns.
---

# CRUD Resource — Psyke Platform

## Architecture layers

Each CRUD resource follows a strict 5-layer stack:

```
controller/  →  service/  →  repository/  →  model/  →  (database)
    dto/   (request/response records)
```

New files go in existing packages. Never create new packages — use `com.br.psyke.psyke.*`.

---

## 1. Model (`model/Entity.java`)

### Conventions
- `@Entity @Table(name = "table_name")` with explicit `@Index`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- UUID PK with `@Id @GeneratedValue(strategy = GenerationType.UUID)`
- `@Column` with explicit `name`, `nullable`, `length`
- `@Enumerated(EnumType.STRING)` for enums
- `@PrePersist` for `createdAt` / `updatedAt` auto-set
- `@Builder.Default` for sensible defaults

### Template
```java
@Entity
@Table(name = "resources", indexes = {
    @Index(name = "idx_resources_clinic", columnList = "clinic_id"),
    @Index(name = "idx_resources_unique_field", columnList = "clinic_id, field")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resource {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResourceStatus status = ResourceStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public enum ResourceStatus { ACTIVE, INACTIVE }
}
```

---

## 2. Repository (`repository/ResourceRepository.java`)

### Conventions
- Extend `JpaRepository<Entity, UUID>`
- Tenant-scoped: `findByClinicId` returning `Page` with `Pageable` parameter
- ID + clinic query: `findByIdAndClinicId(UUID id, UUID clinicId)`
- Uniqueness checks: `existsByClinicIdAndField(clinicId, value)` and `existsByClinicIdAndFieldAndIdNot(clinicId, value, id)`
- Keep it simple — no custom `@Query` unless necessary

### Template
```java
public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    Page<Resource> findByClinicId(UUID clinicId, Pageable pageable);
    Optional<Resource> findByIdAndClinicId(UUID id, UUID clinicId);
    boolean existsByClinicIdAndUniqueField(UUID clinicId, String value);
    boolean existsByClinicIdAndUniqueFieldAndIdNot(UUID clinicId, String value, UUID id);
}
```

---

## 3. DTOs (`dto/*.java`)

### Conventions
- Java `record` with `@Schema` Swagger annotation
- `jakarta.validation` constraints on request DTOs: `@NotBlank`, `@Email`, `@Size`, `@Positive`
- **CreateRequest**: required fields with `@NotBlank`, optional fields nullable
- **UpdateRequest**: all fields nullable (partial update via PATCH/PUT semantics)
- **Response**: includes `id`, all fields, `createdAt`/`updatedAt`

### Templates

**CreateRequest:**
```java
@Schema(description = "Request to create a new resource")
public record CreateResourceRequest(
    @NotBlank @Size(max = 200) @Schema(description = "...", example = "...") String name,
    @Size(max = 20) @Schema(description = "...", example = "...") String optionalField
) {}
```

**UpdateRequest:**
```java
@Schema(description = "Request to update an existing resource")
public record UpdateResourceRequest(
    @Size(max = 200) @Schema(description = "...", example = "...") String name,
    @Schema(description = "...", example = "true") Boolean active
) {}
```

**Response:**
```java
@Schema(description = "Resource response")
public record ResourceResponse(
    String id, String name, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}
```

---

## 4. Service (`service/ResourceService.java`)

### Conventions
- `@Service @RequiredArgsConstructor` with constructor injection
- `private final ResourceRepository variableName;` (pluralized field name)
- **findAll**: returns `Page<Response>` with `Pageable`
- **findById**: `findByIdAndClinicId()` → throw `ResourceNotFoundException`
- **create**: check uniqueness → `Entity.builder().build()` → `repo.save()` → `toResponse()`
- **update**: `findByIdAndClinicId()` → check uniqueness on changed fields → set non-null fields → `repo.save()`
- **delete**: `findByIdAndClinicId()` → `repo.delete()`
- `@Transactional` on write methods
- Uniqueness violations throw `DuplicateResourceException`
- Private `toResponse(Entity)` mapper method

### Duplicate validation snippet
```java
if (req.field() != null && !req.field().isBlank()
        && !req.field().equals(entity.getField())
        && repo.existsByClinicIdAndFieldAndIdNot(clinicId, req.field(), id))
    throw new DuplicateResourceException("Field already registered");
```

---

## 5. Controller (`controller/ResourceController.java`)

### Conventions
- `@RestController @RequestMapping("/resources") @RequiredArgsConstructor`
- `@Tag(name = "Resources", description = "...")`
- Clinic context: `TenantContext.getClinicId()` (UUID)
- **GET list**: `ResponseEntity.ok(service.findAll(clinicId, pageable))` with `Pageable pageable`
- **GET by ID**: `ResponseEntity.ok(service.findById(id, clinicId))`
- **POST**: `ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, clinicId))`
- **PUT**: `ResponseEntity.ok(service.update(id, req, clinicId))`
- **DELETE**: `@PreAuthorize("hasRole('ADMIN')")` → `ResponseEntity.noContent().build()`
- `@Valid @RequestBody` on write endpoints
- `@Operation(summary = "...")` Swagger

---

## 6. Tests (`test/.../ResourceServiceTest.java`)

### Conventions
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` all repositories
- Manual service instantiation in `@BeforeEach`
- AssertJ (`assertThat`, `assertThatThrownBy`)
- Mockito (`when`, `verify`, `any()`)
- Test both success and error paths:
  - `shouldCreate*` — valid creation with saved mock
  - `shouldThrowWhen*AlreadyExists` — duplicate field
  - `shouldFindById` / `shouldThrowWhenNotFound`
  - `shouldFindAllWithPagination` — `PageImpl` mock
  - `shouldUpdate*` — partial field update
  - `shouldThrowOnUpdateWhen*Conflict` — duplicate on update
  - `shouldDelete*` — verify repo.delete() called
  - `shouldThrowOnDeleteWhenNotFound` — empty optional

### Template
```java
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock private ResourceRepository repo;
    private ResourceService service;
    private UUID clinicId;
    private Resource existing;

    @BeforeEach
    void setUp() {
        service = new ResourceService(repo);
        clinicId = UUID.randomUUID();
        existing = Resource.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .name("Existing").status(Resource.ResourceStatus.ACTIVE).build();
    }

    @Test
    void shouldCreate() {
        var req = new CreateResourceRequest("New", null);
        var saved = Resource.builder().id(UUID.randomUUID()).clinicId(clinicId)
                .name("New").status(Resource.ResourceStatus.ACTIVE).build();
        when(repo.save(any())).thenReturn(saved);

        var resp = service.create(req, clinicId);
        assertThat(resp.name()).isEqualTo("New");
    }

    @Test
    void shouldFindAllWithPagination() {
        var pageable = PageRequest.of(0, 10);
        when(repo.findByClinicId(clinicId, pageable))
                .thenReturn(new PageImpl<>(List.of(existing)));

        var resp = service.findAll(clinicId, pageable);
        assertThat(resp).hasSize(1);
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(repo.findByIdAndClinicId(any(), eq(clinicId))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(UUID.randomUUID(), clinicId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

---

## 7. Cross-cutting concerns

### Exceptions available
| Exception | HTTP Status | When to throw |
|-----------|-------------|--------------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `DuplicateResourceException` | 409 | Unique field conflict |
| `SlotConflictException` | 409 | Time slot / room conflict |
| `InvalidTransitionException` | 422 | Invalid state transition |

Multi-tenancy is handled by schema-per-tenant (`TenantContext.getTenantId()`) and clinic-scoped data (`TenantContext.getClinicId()`). All tenant-scoped queries use `clinicId` for data isolation within the schema.

### Pagination
Always return `Page<Response>` from list endpoints, accepting `Pageable pageable` as a controller parameter. Do NOT use unbounded `List<T>` for production endpoints.
