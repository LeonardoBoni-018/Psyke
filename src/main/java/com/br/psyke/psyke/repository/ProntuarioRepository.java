package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProntuarioRepository extends JpaRepository<Prontuario, UUID> {
    Optional<Prontuario> findByPatientId(UUID patientId);
    boolean existsByPatientId(UUID patientId);
}
