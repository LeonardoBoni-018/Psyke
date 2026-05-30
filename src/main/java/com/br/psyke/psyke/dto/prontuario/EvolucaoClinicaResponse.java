package com.br.psyke.psyke.dto.prontuario;

import com.br.psyke.psyke.model.EvolucaoClinica;
import java.util.UUID;

public record EvolucaoClinicaResponse(
    UUID id,
    UUID prontuarioId,
    UUID appointmentId,
    UUID professionalId,
    String subjective,
    String objective,
    String assessment,
    String plan,
    String techniques,
    String createdAt
) {
    public static EvolucaoClinicaResponse from(EvolucaoClinica e) {
        return new EvolucaoClinicaResponse(e.getId(), e.getProntuarioId(), e.getAppointmentId(),
            e.getProfessionalId(), e.getSubjective(), e.getObjective(),
            e.getAssessment(), e.getPlan(), e.getTechniques(),
            e.getCreatedAt().toString());
    }
}
