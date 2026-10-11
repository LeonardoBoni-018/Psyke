package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questionnaire_assignments", indexes = {
    @Index(name = "idx_qassignments_clinic", columnList = "clinic_id"),
    @Index(name = "idx_qassignments_patient", columnList = "patient_id"),
    @Index(name = "idx_qassignments_qid", columnList = "questionnaire_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionnaireAssignment {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "questionnaire_id", nullable = false)
    private UUID questionnaireId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String answers;

    @Column(precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (assignedAt == null) assignedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public enum AssignmentStatus { PENDING, COMPLETED }
}
