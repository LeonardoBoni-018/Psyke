package com.br.psyke.psyke.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_exceptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityException {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ExceptionType type;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean active;

    public enum ExceptionType { VACATION, HOLIDAY, MANUAL_BLOCK, EXTRA_HOUR }
}
