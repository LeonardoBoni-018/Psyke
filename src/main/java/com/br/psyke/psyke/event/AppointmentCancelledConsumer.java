package com.br.psyke.psyke.event;

import com.br.psyke.psyke.model.Slot;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Frees the slot of a cancelled appointment and offers it to the waiting list. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCancelledConsumer {

    private final SlotRepository slotRepo;
    private final WaitingListService waitingListService;

    @Transactional
    public void onCancelled(AppointmentEvent event) {
        log.info("Processing cancellation of appointment {}", event.appointmentId());

        var start = LocalDateTime.parse(event.startTime());
        var end = LocalDateTime.parse(event.endTime());
        var slots = slotRepo.findByProfessionalIdAndStartTimeBetweenOrderByStartTime(
                event.professionalId(), start, end);
        for (var s : slots) {
            s.setStatus(Slot.SlotStatus.FREE);
            s.setPatientId(null);
            s.setRoomId(null);
        }
        slotRepo.saveAll(slots);

        waitingListService.offerNext(event.professionalId(), event.startTime(), event.endTime());
    }
}
