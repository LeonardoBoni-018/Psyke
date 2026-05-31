package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "treatment_plans", indexes = {
    @Index(name = "idx_treatment_plans_clinic", columnList = "clinic_id"),
    @Index(name = "idx_treatment_plans_prontuario", columnList = "prontuario_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreatmentPlan {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "prontuario_id", nullable = false)
    private UUID prontuarioId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String techniques;

    @Column(length = 100)
    private String frequency;

    @Column(name = "estimated_sessions")
    private Integer estimatedSessions;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

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

    public enum PlanStatus { ACTIVE, COMPLETED, SUSPENDED, CANCELLED }
}
