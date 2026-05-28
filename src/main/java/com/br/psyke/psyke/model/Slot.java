package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Slot {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SlotStatus status;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); if (status == null) status = SlotStatus.FREE; }

    public enum SlotStatus { FREE, BOOKED, BLOCKED, CANCELLED }
}
