package com.br.psyke.psyke.event;

import com.br.psyke.psyke.security.TenantScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Default delivery (no Kafka): handlers run asynchronously AFTER the publishing transaction commits,
 * with the publisher's tenant bound. Failures are logged and never affect the HTTP request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "psyke.messaging.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class InProcessEventListeners {

    private final AppointmentNotifier notifier;
    private final AppointmentCancelledConsumer cancelledConsumer;
    private final SessionEventConsumer sessionConsumer;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onAppointment(EventEnvelopes.AppointmentEnvelope envelope) {
        TenantScope.run(envelope.tenantId(), envelope.clinicId(), () -> {
            var event = envelope.event();
            safely("notify " + event.eventType(), () -> notifier.handle(event));
            if ("cancelled".equals(event.eventType()))
                safely("waiting list", () -> cancelledConsumer.onCancelled(event));
        });
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onSession(EventEnvelopes.SessionEnvelope envelope) {
        TenantScope.run(envelope.tenantId(), envelope.clinicId(),
                () -> safely("auto-evolution", () -> sessionConsumer.onSessionCreated(envelope.event())));
    }

    private static void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Event handler '{}' failed: {}", what, e.getMessage(), e);
        }
    }
}
