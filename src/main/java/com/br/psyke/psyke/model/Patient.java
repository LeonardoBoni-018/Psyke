package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patients_clinic", columnList = "clinic_id"),
    @Index(name = "idx_patients_cpf", columnList = "clinic_id, cpf"),
    @Index(name = "idx_patients_email", columnList = "clinic_id, email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "full_name", nullable = false, length = 300)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 14)
    private String cpf;

    @Column(length = 30)
    private String gender;

    @Column(name = "marital_status", length = 30)
    private String maritalStatus;

    @Column(length = 200)
    private String occupation;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String insurance;

    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    @Column(name = "referred_by", length = 200)
    private String referredBy;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PatientStatus status = PatientStatus.ACTIVE;

    @Column(name = "first_appointment")
    private OffsetDateTime firstAppointment;

    @Column(name = "last_appointment")
    private OffsetDateTime lastAppointment;

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

    public enum PatientStatus { ACTIVE, INACTIVE }
}
