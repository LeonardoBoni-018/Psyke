package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    Optional<Clinic> findByIdAndActiveTrue(UUID id);
    boolean existsByCnpj(String cnpj);
    boolean existsByCnpjAndIdNot(String cnpj, UUID id);
}
