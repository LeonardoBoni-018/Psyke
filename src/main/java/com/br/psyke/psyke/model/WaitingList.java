package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "waiting_list")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WaitingList {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    private int priority;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "offered_at")
    private OffsetDateTime offeredAt;

    @Column(name = "offered_until")
    private OffsetDateTime offeredUntil;

    @Column(name = "slot_start_time")
    private OffsetDateTime slotStartTime;

    @Column(name = "slot_end_time")
    private OffsetDateTime slotEndTime;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private WaitingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = WaitingStatus.WAITING;
        if (requestedAt == null) requestedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public enum WaitingStatus { WAITING, OFFERED, ACCEPTED, EXPIRED }
}
