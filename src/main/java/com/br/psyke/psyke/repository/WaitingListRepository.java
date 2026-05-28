package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.WaitingList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitingListRepository extends JpaRepository<WaitingList, UUID> {

    Optional<WaitingList> findByPatientIdAndProfessionalIdAndStatus(
        UUID patientId, UUID professionalId, WaitingList.WaitingStatus status);

    Optional<WaitingList> findFirstByProfessionalIdAndStatusOrderByPriorityDescRequestedAtAsc(
        UUID professionalId, WaitingList.WaitingStatus status);

    List<WaitingList> findByProfessionalIdOrderByPriorityDescRequestedAtAsc(UUID professionalId);

    List<WaitingList> findByStatusAndOfferedUntilBefore(
        WaitingList.WaitingStatus status, OffsetDateTime now);
}
