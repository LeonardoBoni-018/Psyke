package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Anamnese;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AnamneseRepository extends JpaRepository<Anamnese, UUID> {
    Optional<Anamnese> findByProntuarioId(UUID prontuarioId);
}
