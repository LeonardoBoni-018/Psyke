package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.EvolucaoClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvolucaoClinicaRepository extends JpaRepository<EvolucaoClinica, UUID> {
    List<EvolucaoClinica> findByProntuarioIdOrderByCreatedAtDesc(UUID prontuarioId);
    Optional<EvolucaoClinica> findByAppointmentId(UUID appointmentId);
    boolean existsByAppointmentId(UUID appointmentId);
}
