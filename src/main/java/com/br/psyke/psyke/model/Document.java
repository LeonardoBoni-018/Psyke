package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_documents_clinic", columnList = "clinic_id"),
    @Index(name = "idx_documents_patient", columnList = "patient_id"),
    @Index(name = "idx_documents_appointment", columnList = "appointment_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    @Builder.Default
    private long fileSize = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
