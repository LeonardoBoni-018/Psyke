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
public class AppointmentEventProducer {

    private final KafkaTemplate<String, AppointmentEvent> kafka;
    private final ApplicationEventPublisher publisher;

    public AppointmentEventProducer(
            @Qualifier("appointmentKafkaTemplate") ObjectProvider<KafkaTemplate<String, AppointmentEvent>> kafka,
            ApplicationEventPublisher publisher) {
        this.kafka = kafka.getIfAvailable();
        this.publisher = publisher;
    }

    public void publish(AppointmentEvent event) {
        if (kafka == null) {
            publisher.publishEvent(new EventEnvelopes.AppointmentEnvelope(
                    TenantContext.getTenantId(), TenantContext.getClinicId(), event));
            return;
        }
        var topic = "appointment." + event.eventType();
        kafka.send(topic, TenantEventKeys.key(event.appointmentId()), event)
            .whenComplete((r, ex) -> {
                if (ex != null) log.error("Failed to send {}: {}", topic, ex.getMessage());
                else log.debug("Sent {} -> {}", topic, event.appointmentId());
            });
    }
}
