package com.br.psyke.psyke.event;

import com.br.psyke.psyke.security.TenantScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka delivery ({@code psyke.messaging.kafka.enabled=true}). The tenant travels in the record key
 * (see {@link TenantEventKeys}) and is bound before any handler touches tenant tables.
 * One listener per topic, so handlers sharing a topic are never split across a consumer group.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "psyke.messaging.kafka.enabled", havingValue = "true")
public class KafkaEventListeners {

    private final AppointmentNotifier notifier;
    private final AppointmentCancelledConsumer cancelledConsumer;
    private final SessionEventConsumer sessionConsumer;

    @KafkaListener(topics = {"appointment.scheduled", "appointment.confirmed"}, groupId = "psyke-notifications")
    public void onScheduledOrConfirmed(@Payload AppointmentEvent event,
                                       @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        withTenant(key, () -> notifier.handle(event));
    }

    @KafkaListener(topics = "appointment.cancelled", groupId = "psyke-cancellations")
    public void onCancelled(@Payload AppointmentEvent event,
                            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        withTenant(key, () -> {
            notifier.handle(event);
            cancelledConsumer.onCancelled(event);
        });
    }

    @KafkaListener(topics = SessionEventProducer.TOPIC, groupId = "psyke-sessions")
    public void onSession(@Payload SessionEvent event,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        withTenant(key, () -> sessionConsumer.onSessionCreated(event));
    }

    private static void withTenant(String key, Runnable action) {
        var tenantId = TenantEventKeys.tenantOf(key);
        if (tenantId == null) {
            log.warn("Dropping event without tenant (key={})", key);
            return;
        }
        TenantScope.run(tenantId, TenantEventKeys.clinicOf(key), action);
    }
}
