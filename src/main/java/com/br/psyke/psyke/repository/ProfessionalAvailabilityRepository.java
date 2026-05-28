package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.ProfessionalAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalAvailabilityRepository extends JpaRepository<ProfessionalAvailability, UUID> {

    List<ProfessionalAvailability> findByProfessionalIdAndActiveTrueOrderByDayOfWeek(UUID professionalId);

    Optional<ProfessionalAvailability> findByProfessionalIdAndDayOfWeek(UUID professionalId, int dayOfWeek);
}
