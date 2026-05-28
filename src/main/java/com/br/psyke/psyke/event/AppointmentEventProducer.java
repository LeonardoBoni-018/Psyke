package com.br.psyke.psyke.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventProducer {

    private final KafkaTemplate<String, AppointmentEvent> kafka;

    public void publish(AppointmentEvent event) {
        var topic = "appointment." + event.eventType();
        kafka.send(topic, event.appointmentId().toString(), event)
            .whenComplete((r, ex) -> {
                if (ex != null) log.error("Failed to send {}: {}", topic, ex.getMessage());
                else log.info("Sent {} -> {}", topic, event.appointmentId());
            });
    }
}
