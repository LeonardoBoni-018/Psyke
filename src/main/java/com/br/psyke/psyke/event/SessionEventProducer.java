package com.br.psyke.psyke.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionEventProducer {

    private static final String TOPIC = "appointment.session.created";

    @Qualifier("sessionKafkaTemplate")
    private final KafkaTemplate<String, SessionEvent> kafka;

    public SessionEventProducer(KafkaTemplate<String, SessionEvent> kafka) {
        this.kafka = kafka;
    }

    public void publish(SessionEvent event) {
        kafka.send(TOPIC, event.appointmentId().toString(), event)
            .whenComplete((r, ex) -> {
                if (ex != null) log.error("Failed to send {}: {}", TOPIC, ex.getMessage());
                else log.info("Sent {} -> {}", TOPIC, event.appointmentId());
            });
    }
}
