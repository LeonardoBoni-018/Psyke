package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "professionals", indexes = {
    @Index(name = "idx_professionals_clinic", columnList = "clinic_id"),
    @Index(name = "idx_professionals_crp", columnList = "clinic_id, crp")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Professional {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String crp;

    @Column(name = "full_name", nullable = false, length = 300)
    private String fullName;

    @Column(length = 200)
    private String specialty;

    @Column(length = 200)
    private String approach;

    @Column(columnDefinition = "TEXT")
    private String resume;

    @Column(name = "session_value", precision = 10, scale = 2)
    private BigDecimal sessionValue;

    @Column(name = "session_duration", nullable = false)
    @Builder.Default
    private int sessionDuration = 50;

    @Column(name = "accepts_insurance", nullable = false)
    @Builder.Default
    private boolean acceptsInsurance = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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
}
