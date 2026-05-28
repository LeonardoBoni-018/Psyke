package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurrence_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurrenceRule {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(nullable = false, length = 20)
    private String freq;

    @Column(name = "interval_val", nullable = false)
    private int interval;

    private Integer count;

    @Column(name = "until_date")
    private LocalDate until;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (freq == null) freq = "WEEKLY";
        if (interval <= 0) interval = 1;
        active = true;
    }
}
