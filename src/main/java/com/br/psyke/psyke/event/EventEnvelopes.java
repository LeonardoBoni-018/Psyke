package com.br.psyke.psyke.event;

/** In-process event wrappers that remember which tenant raised the event. */
public final class EventEnvelopes {

    private EventEnvelopes() {}

    public record AppointmentEnvelope(String tenantId, String clinicId, AppointmentEvent event) {}

    public record SessionEnvelope(String tenantId, String clinicId, SessionEvent event) {}
}
