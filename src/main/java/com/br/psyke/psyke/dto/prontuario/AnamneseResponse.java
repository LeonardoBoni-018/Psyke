package com.br.psyke.psyke.dto.prontuario;

import com.br.psyke.psyke.model.Anamnese;
import java.util.UUID;

public record AnamneseResponse(
    UUID id,
    UUID prontuarioId,
    UUID professionalId,
    String chiefComplaint,
    String historyIllness,
    String personalHistory,
    String familyHistory,
    String socialHistory,
    String medications,
    String createdAt,
    String updatedAt
) {
    public static AnamneseResponse from(Anamnese a) {
        return new AnamneseResponse(a.getId(), a.getProntuarioId(), a.getProfessionalId(),
            a.getChiefComplaint(), a.getHistoryIllness(), a.getPersonalHistory(),
            a.getFamilyHistory(), a.getSocialHistory(), a.getMedications(),
            a.getCreatedAt().toString(), a.getUpdatedAt().toString());
    }
}
