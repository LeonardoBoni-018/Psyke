package com.br.psyke.psyke.event;

import com.br.psyke.psyke.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionEventProducer {

    static final String TOPIC = "appointment.session.created";

    private final KafkaTemplate<String, SessionEvent> kafka;
    private final ApplicationEventPublisher publisher;

    public SessionEventProducer(
            @Qualifier("sessionKafkaTemplate") ObjectProvider<KafkaTemplate<String, SessionEvent>> kafka,
            ApplicationEventPublisher publisher) {
        this.kafka = kafka.getIfAvailable();
        this.publisher = publisher;
    }

    public void publish(SessionEvent event) {
        if (kafka == null) {
            publisher.publishEvent(new EventEnvelopes.SessionEnvelope(
                    TenantContext.getTenantId(), TenantContext.getClinicId(), event));
            return;
        }
        kafka.send(TOPIC, TenantEventKeys.key(event.appointmentId()), event)
            .whenComplete((r, ex) -> {
                if (ex != null) log.error("Failed to send {}: {}", TOPIC, ex.getMessage());
                else log.debug("Sent {} -> {}", TOPIC, event.appointmentId());
            });
    }
}
