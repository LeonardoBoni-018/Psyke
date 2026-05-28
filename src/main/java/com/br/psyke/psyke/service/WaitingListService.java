package com.br.psyke.psyke.service;

import com.br.psyke.psyke.dto.WaitingListRequest;
import com.br.psyke.psyke.dto.WaitingListResponse;
import com.br.psyke.psyke.event.AppointmentEvent;
import com.br.psyke.psyke.event.AppointmentEventProducer;
import com.br.psyke.psyke.exception.ResourceNotFoundException;
import com.br.psyke.psyke.exception.SlotConflictException;
import com.br.psyke.psyke.model.WaitingList;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.repository.UserRepository;
import com.br.psyke.psyke.repository.WaitingListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingListService {

    private final WaitingListRepository repo;
    private final UserRepository userRepo;
    private final SlotRepository slotRepo;
    private final AppointmentEventProducer eventProducer;

    @Transactional
    public WaitingListResponse join(WaitingListRequest req) {
        userRepo.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        var existing = repo.findByPatientIdAndProfessionalIdAndStatus(
                req.patientId(), req.professionalId(), WaitingList.WaitingStatus.WAITING);
        if (existing.isPresent())
            throw new SlotConflictException("Patient already in waiting list for this professional");

        var entry = WaitingList.builder()
                .patientId(req.patientId())
                .professionalId(req.professionalId())
                .priority(req.priority() != null ? req.priority() : 0)
                .build();
        entry = repo.save(entry);
        return toResponse(entry);
    }

    @Transactional
    public void offerNext(UUID professionalId, String slotStart, String slotEnd) {
        var next = repo.findFirstByProfessionalIdAndStatusOrderByPriorityDescRequestedAtAsc(
                professionalId, WaitingList.WaitingStatus.WAITING);
        if (next.isEmpty()) return;

        var entry = next.get();
        entry.setStatus(WaitingList.WaitingStatus.OFFERED);
        entry.setOfferedAt(OffsetDateTime.now());
        entry.setOfferedUntil(OffsetDateTime.now().plusHours(48));
        entry.setNotifiedAt(OffsetDateTime.now());
        entry.setSlotStartTime(OffsetDateTime.parse(slotStart));
        entry.setSlotEndTime(OffsetDateTime.parse(slotEnd));

        eventProducer.publish(new AppointmentEvent("waiting-list.offer", entry.getId(),
                null, entry.getPatientId(), professionalId, null,
                slotStart, slotEnd, "OFFERED", LocalDateTime.now()));
        log.info("Offered slot to waiting entry {}", entry.getId());
    }

    @Transactional
    public void acceptOffer(UUID entryId) {
        var entry = repo.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Waiting list entry not found"));
        if (entry.getStatus() != WaitingList.WaitingStatus.OFFERED)
            throw new SlotConflictException("Entry is not in OFFERED status");
        if (entry.getOfferedUntil() != null && OffsetDateTime.now().isAfter(entry.getOfferedUntil())) {
            entry.setStatus(WaitingList.WaitingStatus.EXPIRED);
            advanceQueue(entry.getProfessionalId());
            throw new SlotConflictException("Offer has expired");
        }
        entry.setStatus(WaitingList.WaitingStatus.ACCEPTED);
    }

    @Transactional
    public void advanceQueue(UUID professionalId) {
        var next = repo.findFirstByProfessionalIdAndStatusOrderByPriorityDescRequestedAtAsc(
                professionalId, WaitingList.WaitingStatus.WAITING);
        if (next.isEmpty()) return;

        var entry = next.get();
        entry.setStatus(WaitingList.WaitingStatus.OFFERED);
        entry.setOfferedAt(OffsetDateTime.now());
        entry.setOfferedUntil(OffsetDateTime.now().plusHours(48));
        entry.setNotifiedAt(OffsetDateTime.now());
        log.info("Advanced queue to entry {}", entry.getId());
    }

    @Transactional
    public void expireStaleOffers() {
        var stale = repo.findByStatusAndOfferedUntilBefore(
                WaitingList.WaitingStatus.OFFERED, OffsetDateTime.now());
        for (var entry : stale) {
            entry.setStatus(WaitingList.WaitingStatus.EXPIRED);
            log.info("Expired stale offer for entry {}", entry.getId());
            advanceQueue(entry.getProfessionalId());
        }
    }

    public List<WaitingListResponse> listByProfessional(UUID professionalId) {
        return repo.findByProfessionalIdOrderByPriorityDescRequestedAtAsc(professionalId)
                .stream().map(this::toResponse).toList();
    }

    private WaitingListResponse toResponse(WaitingList w) {
        return new WaitingListResponse(w.getId(), w.getPatientId(), w.getProfessionalId(),
                w.getPriority(), w.getRequestedAt().toString(), w.getStatus().name(),
                w.getNotifiedAt() != null ? w.getNotifiedAt().toString() : null,
                w.getOfferedUntil() != null ? w.getOfferedUntil().toString() : null);
    }
}
