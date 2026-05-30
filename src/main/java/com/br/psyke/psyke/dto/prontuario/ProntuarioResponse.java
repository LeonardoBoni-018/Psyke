package com.br.psyke.psyke.dto.prontuario;

import com.br.psyke.psyke.model.Prontuario;
import java.util.UUID;

public record ProntuarioResponse(
    UUID id,
    UUID patientId,
    UUID professionalId,
    String status,
    String allergies,
    String chronicConditions,
    String medications,
    String notes,
    String createdAt,
    String updatedAt
) {
    public static ProntuarioResponse from(Prontuario p) {
        return new ProntuarioResponse(p.getId(), p.getPatientId(), p.getProfessionalId(),
            p.getStatus().name(), p.getAllergies(), p.getChronicConditions(),
            p.getMedications(), p.getNotes(),
            p.getCreatedAt().toString(), p.getUpdatedAt().toString());
    }
}
