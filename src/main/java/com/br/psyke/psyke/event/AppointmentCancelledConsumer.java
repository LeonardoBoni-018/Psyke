package com.br.psyke.psyke.event;

import com.br.psyke.psyke.model.Slot;
import com.br.psyke.psyke.repository.SlotRepository;
import com.br.psyke.psyke.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCancelledConsumer {

    private final SlotRepository slotRepo;
    private final WaitingListService waitingListService;

    @KafkaListener(topics = "appointment.cancelled")
    @Transactional
    public void onCancelled(AppointmentEvent event) {
        log.info("Received cancelled event for appointment {}", event.appointmentId());

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
