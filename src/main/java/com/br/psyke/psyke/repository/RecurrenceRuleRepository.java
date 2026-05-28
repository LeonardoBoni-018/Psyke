package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, UUID> {
    List<RecurrenceRule> findByAppointmentId(UUID appointmentId);
    List<RecurrenceRule> findByActiveTrue();
}
